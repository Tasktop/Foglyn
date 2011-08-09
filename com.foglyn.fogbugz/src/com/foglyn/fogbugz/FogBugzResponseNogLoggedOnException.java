/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

package com.foglyn.fogbugz;

import nu.xom.Document;

public class FogBugzResponseNogLoggedOnException extends FogBugzResponseException {
    private static final long serialVersionUID = 8590183447874952161L;

    public FogBugzResponseNogLoggedOnException(String fogBugzMessage, Document response) {
        super(fogBugzMessage, response);
    }
}
