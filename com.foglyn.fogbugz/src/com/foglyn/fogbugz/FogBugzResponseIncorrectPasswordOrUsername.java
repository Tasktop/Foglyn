package com.foglyn.fogbugz;

import nu.xom.Document;

public class FogBugzResponseIncorrectPasswordOrUsername extends FogBugzResponseException {
    private static final long serialVersionUID = -2425564572853418324L;

    public FogBugzResponseIncorrectPasswordOrUsername(String fogBugzMessage, Document response) {
        super(fogBugzMessage, response);
    }
}
