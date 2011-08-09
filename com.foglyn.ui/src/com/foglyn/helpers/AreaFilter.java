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
