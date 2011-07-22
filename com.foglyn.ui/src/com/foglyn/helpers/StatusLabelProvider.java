package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzStatus;

public class StatusLabelProvider extends TypedLabelProvider<FogBugzStatus> {
    public StatusLabelProvider() {
        super(FogBugzStatus.class);
    }

    @Override
    protected String getTextForElement(FogBugzStatus element) {
        return element.getName();
    }
}
