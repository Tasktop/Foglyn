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

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

class Utils {
    static void assertNotNullArg(Object value, String argName) {
        if (value == null) throw new IllegalArgumentException(argName + " cannot be null");
    }
    
    static Date copyOf(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime());
    }

    static String transformCommonEntities(String input) {
        return input.replace("&amp;", "&");
    }

    static String transformNumericAndCommonEntities(String input) {
        StringBuilder sb = new StringBuilder(input);

        int start = 0;
        for (int ampIndex = sb.indexOf("&", start); ampIndex >= 0; ampIndex = sb.indexOf("&", start)) {
            int semicolon = sb.indexOf(";", ampIndex + 1);
            if (semicolon < 0) {
                break;
            }

            start = semicolon + 1;
            
            String entity = sb.substring(ampIndex + 1, semicolon);
            
            if ("amp".equals(entity)) {
                sb.replace(ampIndex, semicolon + 1, "&");
                start = ampIndex + 1;
                continue;
            }
            
            if (entity.startsWith("#")) {
                try {
                    int v = Integer.valueOf(entity.substring(1));
                    if (v >= 0 && v <= 65535) {
                        char c = (char) v;
                        sb.replace(ampIndex, semicolon + 1, "" + c);
    
                        start = ampIndex + 1;
                        continue;
                    }
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
        }
        
        return sb.toString();
    }
    
    static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot encode to UTF-8? Whoops...", e);
        }
    }

    static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Cannot encode to UTF-8? Whoops...", e);
        }
    }

    /**
     * Closes stream and ignores error while doing so. Don't use with output streams!
     * @param stream
     */
    static void close(Closeable stream) {
        if (stream == null) {
            return;
        }
        
        try {
            stream.close();
        } catch (IOException e) {
            // ignore
        }
    }

    static void checkCancellation(IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    /**
     * Converts response headers into map.
     * 
     * @param method
     * @return Map with header name/value pairs. All header names are in lowercase.
     */
    static Map<String, String> convertHeadersToMap(HttpMethod method) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        
        Header[] headers = method.getResponseHeaders();
        for (Header h: headers) {
            result.put(h.getName().toLowerCase(), h.getValue());
        }
        return result;
    }

    /**
     * Replaces suffix of original string using replacements arrays.
     * 
     * Replacements has pairs in following format: [i] - what to look for, [i+1] - what to replace it for.
     */
    static String replaceSuffix(String original, String... replacements) {
        String lowercase = original.toLowerCase();
        for (int i = 0; i < replacements.length; i = i+2) {
            if (lowercase.endsWith(replacements[i])) {
                return lowercase.substring(0, lowercase.length() - replacements[i].length()) + replacements[i+1];
            }
        }
        
        return original;
    }

    static boolean isEmpty(String caseNumber) {
        return caseNumber == null || caseNumber.trim().length() == 0;
    }
}
