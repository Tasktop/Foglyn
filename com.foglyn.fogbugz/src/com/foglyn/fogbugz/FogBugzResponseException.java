package com.foglyn.fogbugz;

import nu.xom.Document;

/**
 * FogBugz responded with error.
 */
public class FogBugzResponseException extends FogBugzException {
    private static final long serialVersionUID = -1569785796753998393L;

    private final Document response;
    
    public FogBugzResponseException(String fogBugzMessage, Document response) {
        super(fogBugzMessage);
        
        this.response = response;
    }
    
    public Document getFogBugzResponse() {
        return response;
    }
}
