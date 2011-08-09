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
