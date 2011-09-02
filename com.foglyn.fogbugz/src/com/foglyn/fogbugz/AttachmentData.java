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
import java.io.InputStream;

import org.apache.commons.httpclient.methods.multipart.PartSource;

public abstract class AttachmentData {
    PartSource getPartSource() {
        return new PartSource() {
            public InputStream createInputStream() throws IOException {
                return AttachmentData.this.createInputStream();
            }

            public String getFileName() {
                return AttachmentData.this.getFilename();
            }

            public long getLength() {
                return AttachmentData.this.getLength();
            }
        };
    }

    public abstract InputStream createInputStream() throws IOException;
    public abstract String getFilename();
    public abstract String getContentType();
    public abstract long getLength();
}
