package com.foglyn.fogbugz;

import java.util.Map;

/**
 * FogBugz sent unexpected HTTP response (e.g. not 200 status code), or not XML document, ...
 */
public class FogBugzHttpException extends FogBugzCommunicationException {
    private static final long serialVersionUID = -197313407876716586L;
    
    private final int httpCode;
    private final Map<String, String> responseHeaders;
    
    public FogBugzHttpException(String message, String url, int code, Map<String, String> headers) {
        super(message, url);
        
        for (String header: headers.keySet()) {
            if (!header.equals(header.toLowerCase())) {
                throw new IllegalArgumentException("Header names are expected to be in lower case");
            }
        }
        
        this.httpCode = code;
        this.responseHeaders = headers;
    }
    
    public int getHttpCode() {
        return httpCode;
    }
    
    /**
     * @return HTTP Headers from response. Header names are in lowercase.
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
}
