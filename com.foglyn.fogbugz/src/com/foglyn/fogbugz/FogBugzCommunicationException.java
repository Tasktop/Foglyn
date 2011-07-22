package com.foglyn.fogbugz;

/**
 * Indicates invalid response from FogBugz: IOException, not HTTP 200 response, or invalid
 * XML document (when XML document is expected).
 */
public class FogBugzCommunicationException extends FogBugzException {
    private static final long serialVersionUID = -1569785796753998393L;

    /**
     * URL which was accessed when error happened.
     */
    private final String url;
    
    public FogBugzCommunicationException(String message, String url) {
        super(message);
        
        this.url = url;
    }

    public FogBugzCommunicationException(String message, Throwable cause, String url) {
        super(message, cause);
        
        this.url = url;
    }
    
    @Override
    public String toString() {
        return super.toString() + ", url: " + url;
    }
}
