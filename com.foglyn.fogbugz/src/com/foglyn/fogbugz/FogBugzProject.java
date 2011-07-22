package com.foglyn.fogbugz;

import com.foglyn.fogbugz.FogBugzPerson.PersonID;

/**
 * Project has name, owner and <em>inbox flag</em>.
 * 
 * Note: instances of this class are immutable.
 */
public final class FogBugzProject implements HasID <FogBugzProject.ProjectID>{
    public final static class ProjectID extends LongID {
        private ProjectID(long caseID) {
            super(caseID);
        }
        
        public static ProjectID valueOf(String ixProject) {
            return new ProjectID(Long.parseLong(ixProject));
        }
    }

    public static class ProjectIDFactory implements IDFactory<ProjectID> {
        public ProjectID valueOf(String ixProject) {
            return ProjectID.valueOf(ixProject);
        }
    }
    
    private final ProjectID projectID;
    private final String name;
    private final boolean inbox;
    private final PersonID owner;

    public FogBugzProject(ProjectID projectID, String name, boolean inbox, PersonID owner) {
        Utils.assertNotNullArg(projectID, "projectID");
        Utils.assertNotNullArg(name, "name");
        Utils.assertNotNullArg(owner, "owner");
        
        this.projectID = projectID;
        this.name = name;
        this.inbox = inbox;
        this.owner = owner;
    }
    
    public ProjectID getID() {
        return projectID;
    }
    
    public String getName() {
        return name;
    }

    public boolean isInbox() {
        return inbox;
    }
    
    public PersonID getOwner() {
        return owner;
    }
    
    @Override
    public String toString() {
        return "Project: " + name + " (" + projectID + ")";
    }
}
