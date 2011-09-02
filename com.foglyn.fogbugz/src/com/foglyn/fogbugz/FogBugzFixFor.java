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

import java.util.Date;

import com.foglyn.fogbugz.FogBugzProject.ProjectID;

/**
 * FixFor represents date, until case should be fixed.
 * 
 * FixFor has name, optional date, optional project, and can be also deleted.
 * 
 * Note: instances of this class are immutable.
 */
public final class FogBugzFixFor implements HasID<FogBugzFixFor.FixForID>{
    public final static class FixForID extends LongID {
        private FixForID(long fixFoxID) {
            super(fixFoxID);
        }

        public static FixForID valueOf(String ixFixFor) {
            return new FixForID(Long.parseLong(ixFixFor));
        }
    }
    
    public static class FixForIDFactory implements IDFactory<FixForID> {
        public FixForID valueOf(String ixFixFor) {
            return FixForID.valueOf(ixFixFor);
        }
    }
    
    private final FixForID fixForID;
    private final String name;
    private final boolean deleted;
    private final Date date;
    private final ProjectID project;
    
    FogBugzFixFor(FixForID id, String name, boolean deleted, Date date, ProjectID project) {
        Utils.assertNotNullArg(id, "id");
        Utils.assertNotNullArg(name, "name");
        // date can be null
        // project can be null
        
        this.fixForID = id;
        this.name = name;
        this.deleted = deleted;
        this.date = Utils.copyOf(date);
        this.project = project;
    }

    public FixForID getID() {
        return fixForID;
    }

    public String getName() {
        return name;
    }

    /**
     * @return true if this FixFor is deleted, and no new cases should be assigned to it.
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @return date until this FixFor should be completed, or <code>null</code>, if there is no such date.
     */
    public Date getDate() {
        return Utils.copyOf(date);
    }

    /**
     * @return project to which this FixFor is associated, or <code>null</code>,
     *         if this fix for applies for all projects.
     */
    public ProjectID getProject() {
        return project;
    }
    
    @Override
    public String toString() {
        return "Fix for: " + fixForID + " - " + name;
    }
}
