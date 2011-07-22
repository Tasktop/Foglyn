package com.foglyn.fogbugz;

import nu.xom.Document;

public class FogBugzResponseNogLoggedOnException extends FogBugzResponseException {
    private static final long serialVersionUID = 8590183447874952161L;

    public FogBugzResponseNogLoggedOnException(String fogBugzMessage, Document response) {
        super(fogBugzMessage, response);
    }
}
