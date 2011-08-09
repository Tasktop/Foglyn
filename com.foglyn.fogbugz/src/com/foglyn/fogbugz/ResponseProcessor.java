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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.core.runtime.OperationCanceledException;

abstract class ResponseProcessor<T> {

    /**
     * <strong>This method should always process entire input stream, or call abort on http method.</strong>
     * 
     * InputStream is sensitive to cancellation, and will throw {@link OperationCanceledException} when operation is canceled.
     * 
     * @throws OperationCanceledException 
     * @throws IOException 
     * @throws FogBugzException 
     */
    abstract T processResponse(String url, HttpMethod method, InputStream input) throws FogBugzException, IOException, OperationCanceledException;

    boolean checkHttpStatus(int httpCode) {
        return httpCode == HttpStatus.SC_OK;
    }
}
