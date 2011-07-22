package com.foglyn.helpers;

import org.eclipse.jface.viewers.Viewer;

import com.foglyn.fogbugz.FogBugzArea;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;

public class AreaFilter extends TypedFilter<FogBugzArea> {
    private ProjectID projectID;
    
    public AreaFilter() {
        super(FogBugzArea.class);
    }

    public void setProjectID(ProjectID projectId) {
        this.projectID = projectId;
    }
    
    @Override
    protected boolean selectElement(Viewer viewer, Object parentElement, FogBugzArea area) {
        if (projectID == null) {
            return false;
        }
        
        return projectID.equals(area.getProject()); 
    }
}
