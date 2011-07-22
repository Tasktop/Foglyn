package com.foglyn.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.httpclient.HttpClient;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.commons.net.UnsupportedRequestException;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzResponseIncorrectPasswordOrUsername;

/**
 * This class manages FogBugzClient instances.
 * 
 * It keeps clients associated with given repository, and is also able to create new clients.
 * 
 * @author Peter
 *
 */
public class FogBugzClientFactory {
    private final HttpClient httpClient;

    private final Map<Pair<String, AuthenticationCredentials>, FogBugzClient> clients;
    private final AtomicReference<TaskRepositoryLocationFactory> repositoryLocationFactory;
    
    
    FogBugzClientFactory(HttpClient httpClient) {
        Assert.isNotNull(httpClient);
        
        this.httpClient = httpClient;
        this.clients = new HashMap<Pair<String,AuthenticationCredentials>, FogBugzClient>();
        this.repositoryLocationFactory = new AtomicReference<TaskRepositoryLocationFactory>(new TaskRepositoryLocationFactory());
    }

    public synchronized FogBugzClient getFogbugzClient(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
        Pair<String, AuthenticationCredentials> key = repositoryKey(repository);
        
        FogBugzClient client = clients.get(key);
        if (client != null) {
            return client;
        }
        
        AbstractWebLocation loc = repositoryLocationFactory.get().createWebLocation(repository);

        try {
            client = createFogBugzClient(monitor, loc, true, true);
        } catch (FogBugzException e) {
            StatusHandler.log(Utils.toStatus(e));
            
            throw new FoglynCoreException(e);
        }
        
        repository.setProperty(FoglynConstants.REPOSITORY_IS_FOGBUGZ7_REPOSITORY, Boolean.toString(client.isFogBugz7Repository()));
        
        // get current credentials, it might have changed
        key = repositoryKey(repository);
        clients.put(key, client);
        
        return client;
    }

    public void validateCredentials(IProgressMonitor monitor, AbstractWebLocation repositoryLocation) throws FogBugzException, CoreException {
        createFogBugzClient(monitor, repositoryLocation, false, false);
    }
    
    /**
     * Always creates new FogBugzClient. Use {@link #getFogbugzClient(TaskRepository, IProgressMonitor)} to get existing or new client for given repository.
     * 
     * @param monitor
     * @param repositoryLocation
     * @param keepLoggedIn
     * 
     * @return fogbugz client (possibly logged out, if keepLoggedIn is false)
     * 
     * @throws FogBugzException
     * @throws CoreException
     */
    private FogBugzClient createFogBugzClient(IProgressMonitor monitor, AbstractWebLocation repositoryLocation, boolean keepLoggedIn, boolean reauthenticate) throws FogBugzException, CoreException {
        AuthenticationCredentials c = getCredentials(repositoryLocation, monitor, true, "Please enter your username and password");

        FogBugzClient fogBugzClient = FogBugzClient.createFogBugzClient(repositoryLocation, httpClient, monitor);

        boolean loggedIn = false;
        while (!loggedIn) {
            try {
                fogBugzClient.login(c.getUserName(), c.getPassword(), monitor);
                loggedIn = true;
            } catch (FogBugzResponseIncorrectPasswordOrUsername e) {
                if (!reauthenticate) {
                    throw e;
                }
                
                c = getCredentials(repositoryLocation, monitor, false, e.getMessage());
            }
        }
        
        // load caches -- load everything, if keepLoggedIn is true, or load only people if we should check number of users
        if (keepLoggedIn) {
            fogBugzClient.loadCaches(monitor);
        } else {
            fogBugzClient.logout(monitor);
        }
        
        return fogBugzClient;
    }

    private AuthenticationCredentials getCredentials(AbstractWebLocation repositoryLocation, IProgressMonitor monitor, boolean useExisting, String userMessage) throws CoreException {
        AuthenticationCredentials c = null;
        
        if (useExisting) {
            c = repositoryLocation.getCredentials(AuthenticationType.REPOSITORY);
        }
        
        IStatus credsStatus = checkCredentials(c, userMessage);
        while (!credsStatus.isOK()) {
            try {
                repositoryLocation.requestCredentials(AuthenticationType.REPOSITORY, userMessage, monitor);
            } catch (UnsupportedRequestException e) {
                throw new CoreException(credsStatus);
            }
            
            c = repositoryLocation.getCredentials(AuthenticationType.REPOSITORY);
            credsStatus = checkCredentials(c, userMessage);
        }
        
        return c;
    }
    
    // Message parameter is used when credentials are empty
    // Before we try to login, it is "Please enter credentials", after login it may be "Incorrect username or password"
    private IStatus checkCredentials(AuthenticationCredentials creds, String message) throws CoreException {
        if (creds == null) {
            return new Status(IStatus.ERROR, FoglynCorePlugin.PLUGIN_ID, message);
        }
        
        if (creds.getUserName() == null || "".equals(creds.getUserName())) {
            return new Status(IStatus.ERROR, FoglynCorePlugin.PLUGIN_ID, "User name cannot be empty");
        }

        if (creds.getPassword() == null || "".equals(creds.getPassword())) {
            return new Status(IStatus.ERROR, FoglynCorePlugin.PLUGIN_ID, "Password cannot be empty");
        }
        
        return Status.OK_STATUS;
    }

    public void setTaskRepositoryLocationFactory(TaskRepositoryLocationFactory factory) {
        Assert.isNotNull(factory);
        
        this.repositoryLocationFactory.set(factory);
    }
    
    private Pair<String, AuthenticationCredentials> repositoryKey(TaskRepository repository) {
        String url = repository.getRepositoryUrl();
        AuthenticationCredentials creds = repository.getCredentials(AuthenticationType.REPOSITORY);
        
        return new Pair<String, AuthenticationCredentials>(url, creds);
    }
}
