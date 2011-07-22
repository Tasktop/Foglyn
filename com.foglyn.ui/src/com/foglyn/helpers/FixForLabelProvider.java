package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzFixFor;

public class FixForLabelProvider extends TypedLabelProvider<FogBugzFixFor> {
    public FixForLabelProvider() {
        super(FogBugzFixFor.class);
    }

    @Override
    protected String getTextForElement(FogBugzFixFor ff) {
        // FIXME: better fixfor
        return ff.getName();
    }
}
