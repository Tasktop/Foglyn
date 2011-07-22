package com.foglyn.core;

import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

public class FoglynQueryFactory {
    public static FoglynQuery queryInstance(IRepositoryQuery query) {
        String queryType = FilterQuery.getQueryType(query);
        
        FoglynQuery fq = null;
        if (SimpleSearchQuery.matches(queryType)) {
            fq = new SimpleSearchQuery();
        }
        
        if (AdvancedSearchQuery.matches(queryType)) {
            fq = new AdvancedSearchQuery();
        }
        
        if (FilterQuery.matches(queryType)) {
            fq = new FilterQuery();
        }
        
        if (fq == null) return null;
        
        fq.loadFromQuery(query);
        return fq;
    }
}
