package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzPriority;

public class PriorityLabelProvider extends TypedLabelProvider<FogBugzPriority> {
    public PriorityLabelProvider() {
        super(FogBugzPriority.class);
    }

    @Override
    protected String getTextForElement(FogBugzPriority ff) {
        return ff.toUserString();
    }
}
