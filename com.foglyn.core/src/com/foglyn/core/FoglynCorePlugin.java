package com.foglyn.core;

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.WebUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

public class FoglynCorePlugin extends Plugin {
    private static final String USER_AGENT = "Foglyn";

    public static final String PLUGIN_ID = "com.foglyn.core";
    public static final String CONNECTOR_KIND = "foglyn";

    // The shared instance
    private static final AtomicReference<FoglynCorePlugin> plugin = new AtomicReference<FoglynCorePlugin>();

    private final AtomicReference<MultiThreadedHttpConnectionManager> connectionManager;
    private final AtomicReference<HttpClient> httpClient;
    
    private final AtomicReference<FogBugzClientFactory> clientFactory;

    private final AtomicReference<ServiceTracker> proxyServiceTracker;
    
    /**
     * The constructor
     */
    public FoglynCorePlugin() {
        this.connectionManager = new AtomicReference<MultiThreadedHttpConnectionManager>();
        this.httpClient = new AtomicReference<HttpClient>();
        
        this.clientFactory = new AtomicReference<FogBugzClientFactory>(null);
        
        this.proxyServiceTracker = new AtomicReference<ServiceTracker>(null);
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        
        ServiceTracker pt = new ServiceTracker(context, IProxyService.class.getName(), null);
        proxyServiceTracker.set(pt);
        pt.open();
        
        this.connectionManager.set(new MultiThreadedHttpConnectionManager());

        HttpClient hc = createHttpClient(connectionManager.get());
        this.httpClient.set(hc);
        this.clientFactory.set(new FogBugzClientFactory(hc));

        plugin.set(this);
    }
    
    private String getBundleVersion() {
        Dictionary<?, ?> headers = getBundle().getHeaders();
        Object version = headers.get(Constants.BUNDLE_VERSION);
        if (version == null) {
            return null;
        }
        
        return String.valueOf(version);
    }

    public void stop(BundleContext context) throws Exception {
        plugin.set(null);
        
        ServiceTracker st = proxyServiceTracker.getAndSet(null);
        if (st != null) {
            st.close();
        }
        
        super.stop(context);
    }

    public static FoglynCorePlugin getDefault() {
        return plugin.get();
    }

    MultiThreadedHttpConnectionManager getConnectionManager() {
        return connectionManager.get();
    }

    public FogBugzClientFactory getClientFactory() {
        return clientFactory.get();
    }

    private HttpClient createHttpClient(HttpConnectionManager connectionManager) {
        HttpClient httpClient = new HttpClient();
        httpClient.setHttpConnectionManager(connectionManager);
        httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        
        String userAgent = USER_AGENT;
        String version = getBundleVersion();
        if (version != null) {
            userAgent = USER_AGENT + "/" + version;
        }
        
        WebUtil.configureHttpClient(httpClient, userAgent);
        return httpClient;
    }

    public void log(int statusSeverity, String message) {
        getLog().log(new Status(statusSeverity, PLUGIN_ID, message));
    }

    public void log(int statusSeverity, String message, Throwable exception) {
        getLog().log(new Status(statusSeverity, PLUGIN_ID, message, exception));
    }
}
