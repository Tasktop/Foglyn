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
