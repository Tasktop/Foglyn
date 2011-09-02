/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

package com.foglyn.fogbugz;

import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;

/**
 * Projects in FogBugz are divided into areas. Area belongs to project.
 * 
 * Each case has associated project as well as area.
 * Note: instances of this class are immutable.
 */
public final class FogBugzArea implements HasID<FogBugzArea.AreaID> {
    public final static class AreaID extends LongID {
        private AreaID(long caseID) {
            super(caseID);
        }
        
        public static AreaID valueOf(String ixArea) {
            return new AreaID(Long.parseLong(ixArea));
        }
    }
    
    public static class AreaIDFactory implements IDFactory<AreaID> {
        public AreaID valueOf(String ixArea) {
            return AreaID.valueOf(ixArea);
        }
    }
    
    private final AreaID areaID;
    private final String name;
    private final ProjectID projectID;
    private final PersonID owner;

    FogBugzArea(AreaID areaID, String name, ProjectID projectID, PersonID owner) {
        Utils.assertNotNullArg(areaID, "areaID");
        Utils.assertNotNullArg(name, "name");
        Utils.assertNotNullArg(projectID, "projectID");
        // owner can be null
        
        this.areaID = areaID;
        this.name = name;
        this.projectID = projectID;
        this.owner = owner;
    }
    
    public AreaID getID() {
        return areaID;
    }
    
    public String getName() {
        return name;
    }

    /**
     * @return ID of project, where this area belongs
     */
    public ProjectID getProject() {
        return projectID;
    }

    /**
     * @return owner of this area, or <code>null</code> if project's owner should be used instead.
     */
    public PersonID getOwner() {
        return owner;
    }
    
    @Override
    public String toString() {
        return "Area: " + name + " (" + areaID + ")";
    }
}
