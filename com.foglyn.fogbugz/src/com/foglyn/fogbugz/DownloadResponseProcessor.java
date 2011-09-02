/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

package com.foglyn.fogbugz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.core.runtime.OperationCanceledException;

class DownloadResponseProcessor extends ResponseProcessor<Long> {
    private final OutputStream output;
    
    DownloadResponseProcessor(OutputStream output) {
        this.output = output;
    }

    // IOException may be thrown from input stream as well as from output stream
    @Override
    Long processResponse(String url, HttpMethod method, InputStream input) throws FogBugzException, IOException, OperationCanceledException {
        long length = 0;
        
        byte[] buffer = new byte[4096];
        int read = input.read(buffer);
        while (read > 0) {
            length += read;
            
            output.write(buffer, 0, read);
            read = input.read(buffer);
        }
        
        return length;
    }
}
