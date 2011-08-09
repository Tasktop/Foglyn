/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

package com.foglyn.fogbugz;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This input stream support cancellation (via IProgressMonitor) for each
 * operation, even if it takes longer and would block under normal
 * circumstances.
 */
class CancellableInputStream extends InputStream {
    private final InputStream inputStream;
    private final CancellableStreamSupport streamSupport;

    // cached task for reading single byte. (Read to buffer cannot be cached as it needs parameters, and close task is used only once)
    private final ReadCall readTask;
    
    private final Log log = Logging.getLogger("cancellableStream");

    private volatile boolean closed;
    
    public CancellableInputStream(InputStream stream, IProgressMonitor monitor, ThreadFactory factory) {
        Utils.assertNotNullArg(stream, "inputStream");
        
        this.inputStream = stream;
        this.streamSupport = new CancellableStreamSupport(monitor, factory);
        this.readTask = new ReadCall(inputStream);
    }
    
    @Override
    public int read() throws IOException {
        return streamSupport.submit(readTask);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        ReadBuffer readBufferTask = new ReadBuffer(inputStream, b, off, len);
        int result = streamSupport.submit(readBufferTask);

        if (log.isDebugEnabled()) {
            if (result < 0) {
                log.debug("Finished receiving data");
            } else {
                log.debug("Received " + result + " bytes");
            }
        }
        
        return result;
    }
    
    @Override
    public void close() throws IOException {
        // avoid closing this stream again if it is closed already
        if (closed) return;
        
        streamSupport.submit(new CloseCall(inputStream));
        
        closed = true;
        streamSupport.shutdown();
    }
    
    void shutdownBackgroundThread() {
        streamSupport.shutdown();
    }
    
    private final static class ReadBuffer implements Callable<Integer> {
        private final InputStream inputStream;

        private final byte[] buffer;
        private final int length;
        private final int offset;

        ReadBuffer(InputStream is, byte[] buffer, int offset, int length) {
            this.inputStream = is;
            this.length = length;
            this.buffer = buffer;
            this.offset = offset;
        }

        public Integer call() throws IOException {
            return inputStream.read(buffer, offset, length);
        }
    }

    private final static class ReadCall implements Callable<Integer> {
        private final InputStream inputStream;

        ReadCall(InputStream is) {
            inputStream = is;
        }

        public Integer call() throws IOException {
            return inputStream.read();
        }
    }

    private final static class CloseCall implements Callable<Void> {
        private final InputStream inputStream;

        CloseCall(InputStream is) {
            inputStream = is;
        }

        public Void call() throws IOException {
            inputStream.close();
            return null;
        }
    }
}
