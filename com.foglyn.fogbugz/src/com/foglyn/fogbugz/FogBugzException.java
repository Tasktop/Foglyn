package com.foglyn.fogbugz;

/**
 * General exception for all errors while communicating with FogBugz.
 */
public class FogBugzException extends Exception {
	private static final long serialVersionUID = 995017827108541557L;

	public FogBugzException(String message) {
        super(message);
    }

    public FogBugzException(String message, Throwable cause) {
        super(message, cause);
    }
}
