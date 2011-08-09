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
import java.util.Map;

import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.core.runtime.OperationCanceledException;

public class HeadersResponseProcessor extends ResponseProcessor<Map<String, String>> {
    @Override
    Map<String, String> processResponse(String url, HttpMethod method, InputStream input) throws FogBugzException, IOException, OperationCanceledException {
        Map<String, String> result = Utils.convertHeadersToMap(method);

        // read full response (should be null, so just check)
        
        if (input != null) {
            readResponse(input);
        }
        
        return result;
    }

    private void readResponse(InputStream input) throws IOException {
        byte[] buffer = new byte[128];
        int len = input.read(buffer, 0, buffer.length);
        while (len > 0) {
            len = input.read(buffer, 0, buffer.length);
        }
    }
}
