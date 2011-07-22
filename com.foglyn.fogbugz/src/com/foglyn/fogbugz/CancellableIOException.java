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
