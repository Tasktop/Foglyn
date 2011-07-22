package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzCategory;

public class CategoryLabelProvider extends TypedLabelProvider<FogBugzCategory> {
    public CategoryLabelProvider() {
        super(FogBugzCategory.class);
    }

    @Override
    protected String getTextForElement(FogBugzCategory ff) {
        return ff.getName();
    }
}
