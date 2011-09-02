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

package com.foglyn.core;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

import com.foglyn.fogbugz.FogBugzCase;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;

public class SimpleSearchQuery extends FoglynQuery {
    private static final String QUERY_TYPE = "simple-search";
    private static final String ATTR_QUERY_STRING = "query-string";

    static boolean matches(String queryType) {
        return QUERY_TYPE.equals(queryType);
    }

    private String searchString;
    
    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String queryString) {
        this.searchString = queryString;
    }

    @Override
    String getQueryType() {
        return QUERY_TYPE;
    }

    @Override
    void loadQueryParameters(IRepositoryQuery query) {
        searchString = query.getAttribute(ATTR_QUERY_STRING);
    }

    @Override
    void saveQueryParameters(IRepositoryQuery query) {
        query.setAttribute(ATTR_QUERY_STRING, searchString);
    }

    @Override
    public List<FogBugzCase> search(FogBugzClient client, IProgressMonitor monitor) throws FogBugzException {
        return client.search(searchString, monitor, false);
    }
}
