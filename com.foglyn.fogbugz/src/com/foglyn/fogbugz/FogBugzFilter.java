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

package com.foglyn.fogbugz;

public class FogBugzFilter {
    public enum FilterType {
        BUILTIN, SAVED, SHARED, UNKNOWN
    }
    
    /**
     * Filters have String-based IDs, like "ez", "inbox", "2", etc.
     */
    public static class FilterID extends StringID {
        private FilterID(String id) {
            super(id);
        }

        public static FilterID valueOf(String filterID) {
            return new FilterID(filterID);
        }
    }
    
    private final FilterType type;
    private final FilterID filterID;
    private final String description;
    private final boolean current;
    
    public FogBugzFilter(FilterType type, FilterID filterID, String description, boolean current) {
        this.type = type;
        this.filterID = filterID;
        this.description = description;
        this.current = current;
    }
    
    public FilterType getType() {
        return type;
    }
    public FilterID getFilterID() {
        return filterID;
    }
    public String getDescription() {
        return description;
    }

    public boolean isCurrent() {
        return current;
    }
    
    @Override
    public String toString() {
        return "Filter: " + type + ", id=" + filterID + ", description=\"" + description + "\"";
    }
}
