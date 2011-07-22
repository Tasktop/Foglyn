package com.foglyn.core;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

import com.foglyn.fogbugz.FogBugzCase;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;

public abstract class FoglynQuery {
    private static final String ATTR_QUERY_TYPE = "query-type";

    /**
     * Title of the query. Displayed in UI.
     */
    private String queryTitle = null;
    
    public String getQueryTitle() {
        return queryTitle;
    }

    public void setQueryTitle(String queryTitle) {
        this.queryTitle = queryTitle;
    }

    /**
     * Load search parameters from query.
     */
    public final void loadFromQuery(IRepositoryQuery query) {
        queryTitle = query.getSummary();
        
        loadQueryParameters(query);
    }

    /**
     * Save search parameters to query attributes.
     */
    public final void saveToQuery(IRepositoryQuery query) {
        query.setSummary(queryTitle);

        query.setAttribute(ATTR_QUERY_TYPE, getQueryType());
        
        saveQueryParameters(query);
    }

    /**
     * Perform search.
     * 
     * @param client
     * @param monitor
     * @return
     * @throws FogBugzException
     * @throws CoreException
     */
    public abstract List<FogBugzCase> search(FogBugzClient client, IProgressMonitor monitor) throws FogBugzException;
    
    abstract String getQueryType();

    abstract void loadQueryParameters(IRepositoryQuery query);

    abstract void saveQueryParameters(IRepositoryQuery query);
    
    static String getQueryType(IRepositoryQuery query) {
        return query.getAttribute(ATTR_QUERY_TYPE);
    }
}
