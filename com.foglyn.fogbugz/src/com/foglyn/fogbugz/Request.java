package com.foglyn.fogbugz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.zip.GZIPInputStream;

import nu.xom.Document;
import nu.xom.Nodes;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.logging.Log;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.WebUtil;


class Request {
    private final HttpClient httpClient;
    private final AbstractWebLocation repositoryLocation;
    private final ThreadFactory threadFactory;
    
    private final Log log;
    
    private boolean allowGzip = true;
    private boolean checkResponseError = true;
    
    Request(HttpClient client, AbstractWebLocation repositoryLocation) {
        this.httpClient = client;
        this.repositoryLocation = repositoryLocation;
        this.threadFactory = new PrefixedThreadFactory("http-stream");
        
        this.log = Logging.getLogger("request");
    }
    
    private <T> T request(String url, HttpMethod method, IProgressMonitor monitor, ResponseProcessor<T> processor) throws FogBugzException {
        Utils.checkCancellation(monitor);
        
        HostConfiguration hostConfiguration = WebUtil.createHostConfiguration(httpClient, repositoryLocation, monitor);

        if (allowGzip) {
            method.addRequestHeader("Accept-Encoding", "gzip");
        }

        InputStream responseStream = null;
        CancellableInputStream cancellableStream = null;
        try {
            log.debug("Sending request to server");
            int code = WebUtil.execute(httpClient, hostConfiguration, method, monitor);
            
            log.debug("Got " + code + " response");
            
            if (!processor.checkHttpStatus(code)) {
                Map<String, String> headers = Utils.convertHeadersToMap(method);
                
                method.abort();
                
                throw unexpectedStatus(code, url, headers);
            }

            log.debug("Downloading data");
            
            responseStream = method.getResponseBodyAsStream();
            
            InputStream processed = responseStream;
            
            // may be null, for example for HEAD request
            if (processed != null) {
                Header contentEncoding = method.getResponseHeader("Content-Encoding");
                if (allowGzip && contentEncoding != null && "gzip".equals(contentEncoding.getValue())) {
                    processed = new GZIPInputStream(processed);
                }
                
                cancellableStream = new CancellableInputStream(processed, monitor, threadFactory);
                processed = cancellableStream;
            }
            
            log.debug("Processing response");
            
            return processor.processResponse(url, method, processed);
        } catch (RuntimeException e) {
            // also catches OperationCanceledException
            
            // we don't know what happened to method, so we better abort processing
            method.abort();
            
            log.error("Error while executing request", e);
            
            throw e;
        } catch (IOException e) {
            // we don't know what happened... better abort connection
            method.abort();

            log.error("IO Error while executing request", e);
            
            throw ioError(e, url);
        } finally {
            if (cancellableStream != null) {
                cancellableStream.shutdownBackgroundThread();
            }

            // don't use cancellable stream to close responseStream -- because in case of cancelled monitor, it would ignore close request 
            Utils.close(responseStream);
            
            method.releaseConnection();
        }
    }
    
    Document requestAPI(String url, IProgressMonitor monitor) throws FogBugzException {
        GetMethod method = new GetMethod(url);

        Document result = request(url, method, monitor, new DocumentResponseProcessor());

        if (checkResponseError) {
            throwExceptionOnError(result);
        }
        
        return result;
    }

    /**
     * Returns headers returned by server for given URL. We use HEAD request. Keys in result map are lower case.
     */
    Map<String, String> getHeaders(String url, IProgressMonitor monitor) throws FogBugzException {
        HeadMethod method = new HeadMethod(url);
        
        return request(url, method, monitor, new HeadersResponseProcessor());
    }

    long download(String url, OutputStream output, IProgressMonitor monitor) throws FogBugzException {
        GetMethod method = new GetMethod(url);

        return request(url, method, monitor, new DownloadResponseProcessor(output));
    }

    Document post(String url, List<Part> parts, IProgressMonitor monitor) throws FogBugzException {
        PostMethod postMethod = new PostMethod(url);
        // postMethod.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);
        postMethod.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[0]), postMethod.getParams()));

        Document result = request(url, postMethod, monitor, new DocumentResponseProcessor());

        if (checkResponseError) {
            throwExceptionOnError(result);
        }
        
        return result;
    }

    private void throwExceptionOnError(Document result) throws FogBugzException {
        Nodes nodes = result.query("/response/error");
        if (nodes.size() == 0) {
            return;
        }

        String message = XOMUtils.xpathValueOf(result, "/response/error");
        
        String code = XOMUtils.xpathValueOf(result, "/response/error/@code");
        
        if ("1".equals(code)) {
            throw new FogBugzResponseIncorrectPasswordOrUsername(message, result);
        }
        if ("3".equals(code)) {
            throw new FogBugzResponseNogLoggedOnException(message, result);
        }
        if ("7".equals(code)) {
            throw new FogBugzResponseTimeTrackingException(message, result);
        }
        if ("9".equals(code)) {
            throw new FogBugzResponseCaseHasBeenChangedException(message, result);
        }
        
        throw new FogBugzResponseException(message, result);
    }

    private FogBugzCommunicationException unexpectedStatus(int code, String url, Map<String, String> headers) {
        return new FogBugzHttpException("FogBugz responded with unexpected HTTP response code: " + code, url, code, headers);
    }
    
    private FogBugzCommunicationException ioError(IOException e, String url) {
        if (e instanceof ConnectException) {
            return new FogBugzCommunicationException("Unable to connect to FogBugz server, server is down", e, url);
        }
        
        if (e instanceof NoRouteToHostException) {
            return new FogBugzCommunicationException("Unable to connect to FogBugz server, no route to host", e, url);
        }
        
        if (e instanceof UnknownHostException) {
            return new FogBugzCommunicationException("Unable to connect to FogBugz server, unknown host: " + e.getMessage(), e, url);
        }
        
        return new FogBugzCommunicationException("IO Error while communicating with FogBugz", e, url);
    }
}
