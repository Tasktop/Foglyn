package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzFilter;
import com.foglyn.fogbugz.FogBugzFilter.FilterType;

public class FilterLabelProvider extends TypedLabelProvider<FogBugzFilter> {
    public FilterLabelProvider() {
        super(FogBugzFilter.class);
    }

    @Override
    protected String getTextForElement(FogBugzFilter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append(filter.getDescription());

        if (filter.getType() == FilterType.SHARED) {
            sb.append(" (shared)");
        }
        
        return sb.toString();
    }
}
