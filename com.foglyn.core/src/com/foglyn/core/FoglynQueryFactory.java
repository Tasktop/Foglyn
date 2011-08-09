/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

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
