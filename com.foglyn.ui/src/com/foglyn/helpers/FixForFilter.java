package com.foglyn.helpers;

import org.eclipse.jface.viewers.Viewer;

import com.foglyn.fogbugz.FogBugzFixFor;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;

public class FixForFilter extends TypedFilter<FogBugzFixFor> {
    private ProjectID projectID;
    
    public FixForFilter() {
        super(FogBugzFixFor.class);
    }

    public void setProjectID(ProjectID projectId) {
        this.projectID = projectId;
    }
    
    @Override
    protected boolean selectElement(Viewer viewer, Object parentElement, FogBugzFixFor fixFor) {
        if (fixFor.getProject() == null) {
            return true;
        }
        
        if (projectID == null) {
            return false;
        }
        
        return projectID.equals(fixFor.getProject()); 
    }
}
