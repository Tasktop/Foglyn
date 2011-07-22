package com.foglyn.fogbugz;

import nu.xom.Document;

public class FogBugzResponseTimeTrackingException extends FogBugzResponseException {
    private static final long serialVersionUID = 2320491838839613734L;

    public FogBugzResponseTimeTrackingException(String fogBugzMessage, Document response) {
        super(fogBugzMessage, response);
    }
}
