package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzArea;

public class AreaLabelProvider extends TypedLabelProvider<FogBugzArea> {
    public AreaLabelProvider() {
        super(FogBugzArea.class);
    }

    @Override
    protected String getTextForElement(FogBugzArea ff) {
        return ff.getName();
    }
}
