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
