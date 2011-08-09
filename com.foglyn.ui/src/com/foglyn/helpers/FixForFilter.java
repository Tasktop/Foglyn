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
