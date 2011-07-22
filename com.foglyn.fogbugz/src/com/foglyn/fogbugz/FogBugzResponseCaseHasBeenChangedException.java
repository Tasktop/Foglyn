package com.foglyn.fogbugz;

import nu.xom.Document;

/**
 * 9 - Case has changed since last view.
 */
public class FogBugzResponseCaseHasBeenChangedException extends FogBugzResponseException {
	private static final long serialVersionUID = -7009539242910014126L;

	public FogBugzResponseCaseHasBeenChangedException(String fogBugzMessage, Document response) {
        super(fogBugzMessage, response);
    }
}
