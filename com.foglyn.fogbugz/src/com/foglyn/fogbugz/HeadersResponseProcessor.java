package com.foglyn.fogbugz;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.core.runtime.OperationCanceledException;

public class HeadersResponseProcessor extends ResponseProcessor<Map<String, String>> {
    @Override
    Map<String, String> processResponse(String url, HttpMethod method, InputStream input) throws FogBugzException, IOException, OperationCanceledException {
        Map<String, String> result = Utils.convertHeadersToMap(method);

        // read full response (should be null, so just check)
        
        if (input != null) {
            readResponse(input);
        }
        
        return result;
    }

    private void readResponse(InputStream input) throws IOException {
        byte[] buffer = new byte[128];
        int len = input.read(buffer, 0, buffer.length);
        while (len > 0) {
            len = input.read(buffer, 0, buffer.length);
        }
    }
}
