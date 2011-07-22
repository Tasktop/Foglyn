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
