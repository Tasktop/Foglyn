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

public class FogBugzAttachment {
    private final String filename;
    
    private final String urlComponent;

    // URL including host, but without token.
    private String urlWithHost;
    
    // fetched in another HTTP request
    private long length;
    private String mimetype;
    
    public FogBugzAttachment(String filename, String url) {
        this.filename = filename;
        this.urlComponent = url;
    }

    /**
     * @return Attachment filename.
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * @return URL part as returned by FogBugz. Example:
     *         <code>"default.asp?pg=pgDownload&pgType=pgFile&ixBugEvent=106&ixAttachment=4&sFileName=errorwindow.png".</code>
     *         This is without host and token.
     */
    public String getUrlComponent() {
        return urlComponent;
    }

    /**
     * @return {@link #getUrlComponent() URL Component} with host, but without
     *         token. Note: This value is computed, and is NOT returned by
     *         FogBugz server.
     */
    public String getUrlWithHost() {
        return urlWithHost;
    }

    public void setUrlWithHost(String url) {
        this.urlWithHost = url;
    }

    /**
     * @return length of attached file. Note: this value is obtained by issuing
     *         HEAD request to server at given URL.
     */
    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    /**
     * @return content type of attached file. Note: this value is obtained by issuing
     *         HEAD request to server at given URL.
     */
    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }
}
