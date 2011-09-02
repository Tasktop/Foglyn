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

import java.io.IOException;

public class CancellableIOException extends IOException {
    private static final long serialVersionUID = -4551509891209223286L;
    
    private final Throwable cause;
    
    public CancellableIOException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }
    
    @Override
    public Throwable getCause() {
        return cause;
    }
}
