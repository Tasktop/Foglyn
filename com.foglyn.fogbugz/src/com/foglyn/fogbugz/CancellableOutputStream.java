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
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This output stream support cancellation (via IProgressMonitor) for each
 * operation, even if it takes longer and would block under normal
 * circumstances.
 */
class CancellableOutputStream extends OutputStream {
    private final CancellableStreamSupport streamSupport;
    private final OutputStream outputStream;
    
    private volatile boolean closed = false;
    
    public CancellableOutputStream(OutputStream stream, IProgressMonitor monitor, ThreadFactory factory) {
        Utils.assertNotNullArg(stream, "outputStream");

        this.streamSupport = new CancellableStreamSupport(monitor, factory);
        this.outputStream = stream;
    }

    @Override
    public void write(int b) throws IOException {
        streamSupport.submit(new WriteByte(outputStream, b));
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        streamSupport.submit(new WriteBuffer(outputStream, b, off, len));
    }
    
    @Override
    public void flush() throws IOException {
        streamSupport.submit(new Flush(outputStream));
    }
    
    @Override
    public void close() throws IOException {
        // avoid closing this stream again if it is closed already
        if (closed) return;
        
        streamSupport.submit(new CloseCall(outputStream));
        
        closed = true;
        streamSupport.shutdown();
    }
    
    void shutdownBackgroundThread() {
        streamSupport.shutdown();
    }
    
    private final static class WriteBuffer implements Callable<Void> {
        private final OutputStream outputStream;

        private final byte[] buffer;
        private final int length;
        private final int offset;

        WriteBuffer(OutputStream os, byte[] buffer, int offset, int length) {
            this.outputStream = os;
            this.length = length;
            this.buffer = buffer;
            this.offset = offset;
        }

        public Void call() throws IOException {
            outputStream.write(buffer, offset, length);
            return null;
        }
    }

    private final static class WriteByte implements Callable<Void> {
        private final OutputStream outputStream;
        private final int b;

        WriteByte(OutputStream os, int b) {
            this.outputStream = os;
            this.b = b;
        }

        public Void call() throws IOException {
            outputStream.write(b);
            return null;
        }
    }

    private final static class CloseCall implements Callable<Void> {
        private final OutputStream outputStream;

        CloseCall(OutputStream os) {
            outputStream = os;
        }

        public Void call() throws IOException {
            outputStream.close();
            return null;
        }
    }

    private final static class Flush implements Callable<Void> {
        private final OutputStream outputStream;

        Flush(OutputStream os) {
            outputStream = os;
        }

        public Void call() throws IOException {
            outputStream.flush();
            return null;
        }
    }
}
