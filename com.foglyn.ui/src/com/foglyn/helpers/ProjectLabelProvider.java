package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzProject;

public class ProjectLabelProvider extends TypedLabelProvider<FogBugzProject> {
    public ProjectLabelProvider() {
        super(FogBugzProject.class);
    }

    @Override
    protected String getTextForElement(FogBugzProject ff) {
        return ff.getName();
    }
}
