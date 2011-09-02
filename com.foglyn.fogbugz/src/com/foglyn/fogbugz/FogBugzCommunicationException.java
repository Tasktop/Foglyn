/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

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
