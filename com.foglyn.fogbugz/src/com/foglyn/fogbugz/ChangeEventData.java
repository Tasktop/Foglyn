package com.foglyn.fogbugz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzCase.CaseID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzEvent.EventID;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;

public class ChangeEventData {
    private CaseID caseID = null;
    private EventID eventID = null; // for verification if we have up-to-date version of case. If null, not passed to server (no verification will be done).
    
    // following values can be send to FogBugz. If value is null, it is not send and changed.
    private String newTitle = null;
    private ProjectID newProjectID = null;
    private AreaID newAreaID = null;
    private FixForID newFixForID = null;
    private CategoryID newCategoryID = null;
    private PersonID newAssignedTo = null;
    private PriorityID newPriorityID = null;
    private Date newDueDate = null;
    private DaysHoursMinutes newCurrentHoursEstimate = null;
    private DaysHoursMinutes newElapsedTime = null;
    private String newVersion = null; // custom field 1
    private String newComputer = null; // custom field 2
    private String newComment = null; // sEvent
    
    // for resolve
    private StatusID newStatus = null;
    
    private List<AttachmentData> attachments = null;

    private CaseID parentCaseID = null;
    
    private List<String> tags = null;
    
    public CaseID getCaseID() {
        return caseID;
    }

    public void setCaseID(CaseID caseID) {
        this.caseID = caseID;
    }

    public EventID getEventID() {
        return eventID;
    }

    public void setEventID(EventID eventID) {
        this.eventID = eventID;
    }

    public String getNewTitle() {
        return newTitle;
    }

    public void setNewTitle(String newTitle) {
        this.newTitle = newTitle;
    }

    public ProjectID getNewProjectID() {
        return newProjectID;
    }

    public void setNewProjectID(ProjectID newProjectID) {
        this.newProjectID = newProjectID;
    }

    public AreaID getNewAreaID() {
        return newAreaID;
    }

    public void setNewAreaID(AreaID newAreaID) {
        this.newAreaID = newAreaID;
    }

    public FixForID getNewFixForID() {
        return newFixForID;
    }

    public void setNewFixForID(FixForID newFixForID) {
        this.newFixForID = newFixForID;
    }

    public CategoryID getNewCategoryID() {
        return newCategoryID;
    }

    public void setNewCategoryID(CategoryID newCategoryID) {
        this.newCategoryID = newCategoryID;
    }

    public PersonID getNewAssignedTo() {
        return newAssignedTo;
    }

    public void setNewAssignedTo(PersonID newAssignedTo) {
        this.newAssignedTo = newAssignedTo;
    }

    public PriorityID getNewPriorityID() {
        return newPriorityID;
    }

    public void setNewPriorityID(PriorityID newPriorityID) {
        this.newPriorityID = newPriorityID;
    }

    public Date getNewDueDate() {
        return Utils.copyOf(newDueDate);
    }

    public void setNewDueDate(Date newDueDate) {
        this.newDueDate = Utils.copyOf(newDueDate);
    }

    public DaysHoursMinutes getNewCurrentHoursEstimate() {
        return newCurrentHoursEstimate;
    }

    public void setNewCurrentEstimate(DaysHoursMinutes newCurrentEstimate) {
        this.newCurrentHoursEstimate = newCurrentEstimate;
    }

    public DaysHoursMinutes getNewElapsedTime() {
        return newElapsedTime;
    }
    
    public void setNewElapsedTime(DaysHoursMinutes newElapsedTime) {
        this.newElapsedTime = newElapsedTime;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public String getNewComputer() {
        return newComputer;
    }

    public void setNewComputer(String newComputer) {
        this.newComputer = newComputer;
    }

    public String getNewComment() {
        return newComment;
    }

    public void setNewComment(String newComment) {
        this.newComment = newComment;
    }

    public StatusID getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(StatusID newStatus) {
        this.newStatus = newStatus;
    }

    public List<AttachmentData> getAttachments() {
        if (attachments == null) {
            return Collections.emptyList();
        }
        return attachments;
    }

    public void addAttachment(AttachmentData attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("attachment cannot be null");
        }
        
        if (attachments == null) {
            attachments = new ArrayList<AttachmentData>();
        }
        
        attachments.add(attachment);
    }

    public CaseID getParentCaseID() {
        return parentCaseID;
    }

    public void setParentCaseID(CaseID parentCaseID) {
        this.parentCaseID = parentCaseID;
    }

    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
