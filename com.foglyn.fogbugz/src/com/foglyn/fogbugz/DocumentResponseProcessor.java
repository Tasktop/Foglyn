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

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;

import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.core.runtime.OperationCanceledException;

public class DocumentResponseProcessor extends ResponseProcessor<Document> {
    @Override
    Document processResponse(String url, HttpMethod method, InputStream input) throws FogBugzException, IOException, OperationCanceledException {
        Builder builder = new Builder();
        try {
            return builder.build(input);
        } catch (ParsingException e) {
            // abort method, because full response might have not been read
            method.abort();
            
            throw parsingError(e, url);
        }
    }

    private FogBugzCommunicationException parsingError(ParsingException e, String url) {
        // XOM catches runtime exceptions and wraps them as parsing errors. OperationCanceledException isn't error though,
        // so we ignore parsing exception, and rethrow original operation cancelled 
        if (e.getCause() instanceof OperationCanceledException) {
            throw (OperationCanceledException) e.getCause();
        }
        
        return new FogBugzCommunicationException("Cannot parse FogBugz response", e, url);
    }

}
