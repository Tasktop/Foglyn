package com.foglyn.core;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

import com.foglyn.fogbugz.FogBugzCase;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzFilter.FilterID;

public class FilterQuery extends FoglynQuery {
    private static final String QUERY_TYPE = "filter";
    private static final String ATTR_FILTER_ID = "filter-id";

    static boolean matches(String queryType) {
        return QUERY_TYPE.equals(queryType);
    }
    
    private FilterID filterID;
    
    public FilterID getFilterID() {
        return filterID;
    }

    public void setFilterID(FilterID filterId) {
        filterID = filterId;
    }

    @Override
    String getQueryType() {
        return QUERY_TYPE;
    }

    @Override
    void loadQueryParameters(IRepositoryQuery query) {
        String fid = query.getAttribute(ATTR_FILTER_ID);
        filterID = FilterID.valueOf(fid);
    }

    @Override
    void saveQueryParameters(IRepositoryQuery query) {
        query.setAttribute(ATTR_FILTER_ID, filterID.toString());
    }

    @Override
    public List<FogBugzCase> search(FogBugzClient client, IProgressMonitor monitor) throws FogBugzException {
        return client.listTasksForFilter(filterID, monitor, false);
    }
}
