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

package com.foglyn.fogbugz;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzEvent.EventID;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;

public class FogBugzCase {
    public final static class CaseID extends LongID {
        private CaseID(long caseID) {
            super(caseID);
        }

        /**
         * @param ixBug
         * @return new CaseID from string
         * @throws IllegalArgumentException if parameter is not valid case ID
         */
        public static CaseID valueOf(String ixBug) {
            return new CaseID(Long.parseLong(ixBug));
        }
    }
    
    public static class CaseIDFactory implements IDFactory<CaseID> {
        public CaseID valueOf(String ixArea) {
            return CaseID.valueOf(ixArea);
        }
    }

    private CaseID caseID;
    
    private EventID latestEvent;
    
    /**
     * Title of case
     */
    private String title;
    
    /**
     * Direct URL to case (computed)
     */
    private String taskURL;
    
    /**
     * Is case open or closed?
     */
    private boolean open;
    
    private Date openedDate;
    
    private Date closedDate;
    
    private Date resolvedDate;
    
    private Date lastUpdated;
    
    private Date dueDate;
    
    private PersonID assignedTo;
    private PersonID openedBy;

    // can be null if not resolved yet
    private PersonID resolvedBy;
    
    // can be null if not closed yet
    private PersonID closedBy;
    
    private ProjectID project;
    
    private AreaID area;
    
    private StatusID status;
    
    private CategoryID category;
    
    private PriorityID priority;

    private FixForID fixFor;

    private List<FogBugzEvent> caseEvents;

    private Set<CaseAction> actions;

    private BigDecimal originalEstimateInHours;
    
    private BigDecimal currentEstimateInHours;
    
    private BigDecimal elapsedTimeInHours;

    private BigDecimal remainingTimeInHours;
    
    private DaysHoursMinutes convertedOriginalEstimate;
    
    private DaysHoursMinutes convertedCurrentEstimate;
    
    private DaysHoursMinutes convertedElapsedTime;
    
    private DaysHoursMinutes convertedRemainingTime;
    
    private List<CaseID> relatedBugs;
    
    private List<String> tags;
    
    private CaseID parentCase;
    
    private List<CaseID> childrenCases;
    
    /**
     * Case ID
     */
    public CaseID getCaseID() {
        return caseID;
    }

    public void setCaseID(CaseID caseID) {
        this.caseID = caseID;
    }

    /**
     * @return title of the case
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return URL pointing to case details in FogBugz web interface (computed)
     */
    public String getTaskURL() {
        return taskURL;
    }

    public void setTaskURL(String taskURL) {
        this.taskURL = taskURL;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setOpenedDate(Date openedDate) {
        this.openedDate = Utils.copyOf(openedDate);
    }

    public Date getOpenedDate() {
        return Utils.copyOf(openedDate);
    }

    public PersonID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(PersonID openedBy) {
        this.assignedTo = openedBy;
    }

    public PersonID getOpenedBy() {
        return openedBy;
    }

    public void setOpenedBy(PersonID openedBy) {
        this.openedBy = openedBy;
    }

    public PersonID getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(PersonID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public PersonID getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(PersonID closedBy) {
        this.closedBy = closedBy;
    }

    public void setProject(ProjectID project) {
        this.project = project;
    }

    public ProjectID getProject() {
        return project;
    }

    public AreaID getArea() {
        return area;
    }

    public void setArea(AreaID area) {
        this.area = area;
    }

    public void setStatus(StatusID status) {
        this.status = status;
    }

    public StatusID getStatus() {
        return status;
    }

    public void setCategory(CategoryID category) {
        this.category = category;
    }

    public CategoryID getCategory() {
        return category;
    }

    public void setPriority(PriorityID priority) {
        this.priority = priority;
    }

    public PriorityID getPriority() {
        return priority;
    }

    public void setFixFor(FixForID fixFor) {
        this.fixFor = fixFor;
    }

    public FixForID getFixFor() {
        return fixFor;
    }

    public void setClosedDate(Date closedDate) {
        this.closedDate = Utils.copyOf(closedDate);
    }

    /**
     * @return date, when case was closed, or <code>null</code> if case was not
     *         yet closed. Note: case might have been resolved in the past, but
     *         can be still open now, if it was reopened.
     */
    public Date getClosedDate() {
        return Utils.copyOf(closedDate);
    }

    public void setResolvedDate(Date resolvedDate) {
        this.resolvedDate = Utils.copyOf(resolvedDate);
    }

    /**
     * @return date when this case was resolved, or <code>null</code> if this
     *         case was not yet resolved. Note: case might have been resolved in
     *         the past, but can be still open now, if it was reactivated.
     */
    public Date getResolvedDate() {
        return Utils.copyOf(resolvedDate);
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = Utils.copyOf(lastUpdated);
    }

    public Date getLastUpdated() {
        return Utils.copyOf(lastUpdated);
    }

    public Date getDueDate() {
        return Utils.copyOf(dueDate);
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = Utils.copyOf(dueDate);
    }

    public void setEvents(List<FogBugzEvent> caseEvents) {
        this.caseEvents = caseEvents;
    }

    /**
     * @return list of case events, or <code>null</code> if events were not fetched from server
     */
    public List<FogBugzEvent> getCaseEvents() {
        return caseEvents;
    }

    public void setActions(Set<CaseAction> actions) {
        this.actions = actions;
    }
    
    public Set<CaseAction> getActions() {
        return actions;
    }
    
    /**
     * @return ID of latest event in this case
     */
    public EventID getLatestEvent() {
        return latestEvent;
    }
    
    public void setLatestEvent(EventID latestEvent) {
        this.latestEvent = latestEvent;
    }

    /**
     * @return Original estimate in hours, or <code>null</code> if this case has no original estimate.
     */
    public BigDecimal getOriginalEstimateInHours() {
        return originalEstimateInHours;
    }

    public void setOriginalEstimateInHours(BigDecimal originalEstimateInHours) {
        this.originalEstimateInHours = originalEstimateInHours;
    }

    /**
     * @return Current estimate in hours, or <code>null</code> if this case has no current estimate.
     */
    public BigDecimal getCurrentEstimateInHours() {
        return currentEstimateInHours;
    }

    public void setCurrentEstimateInHours(BigDecimal currentEstimateInHours) {
        this.currentEstimateInHours = currentEstimateInHours;
    }

    /**
     * @return Elapsed time in hours, or <code>null</code> if this case has no elapsed time tracked.
     */
    public BigDecimal getElapsedTimeInHours() {
        return elapsedTimeInHours;
    }

    public void setElapsedTimeInHours(BigDecimal elapsedTimeInHours) {
        this.elapsedTimeInHours = elapsedTimeInHours;
    }

    /**
     * @return remaining time in hours, or <code>null</code> if this case has no
     *         current estimate (remaining time cannot be computed in such case)
     */
    public BigDecimal getRemainingTimeInHours() {
        return remainingTimeInHours;
    }

    public void setRemainingTimeInHours(BigDecimal remainingTimeInHours) {
        this.remainingTimeInHours = remainingTimeInHours;
    }

    /**
     * @return Original estimate in (days/hours/minute), or <code>null</code> if this case has no original estimate.
     */
    public DaysHoursMinutes getConvertedOriginalEstimate() {
        return convertedOriginalEstimate;
    }

    public void setConvertedOriginalEstimate(DaysHoursMinutes convertedOriginalEstimate) {
        this.convertedOriginalEstimate = convertedOriginalEstimate;
    }

    /**
     * @return Current estimate in (days/hours/minute), or <code>null</code> if this case has no current estimate.
     */
    public DaysHoursMinutes getConvertedCurrentEstimate() {
        return convertedCurrentEstimate;
    }

    public void setConvertedCurrentEstimate(DaysHoursMinutes convertedCurrentEstimate) {
        this.convertedCurrentEstimate = convertedCurrentEstimate;
    }

    /**
     * @return Elapsed time in (days/hours/minute), or <code>null</code> if this case has no elapsed time.
     */
    public DaysHoursMinutes getConvertedElapsedTime() {
        return convertedElapsedTime;
    }

    public void setConvertedElapsedTime(DaysHoursMinutes convertedElapsedTime) {
        this.convertedElapsedTime = convertedElapsedTime;
    }

    /**
     * @return Remaining time in (days/hours/minute), or <code>null</code> if this case has no current estimate (and remaining time).
     */
    public DaysHoursMinutes getConvertedRemainingTime() {
        return convertedRemainingTime;
    }

    public void setConvertedRemainingTime(DaysHoursMinutes convertedRemainingTime) {
        this.convertedRemainingTime = convertedRemainingTime;
    }
    
    public void setRelatedBugs(List<CaseID> relatedBugs) {
        this.relatedBugs = relatedBugs;
    }
    
    /**
     * @return bugs related to this case (or empty list if none)
     */
    public List<CaseID> getRelatedBugs() {
        if (relatedBugs == null) return Collections.emptyList();
        
        return relatedBugs;
    }

    public void setChildrenCases(List<CaseID> childrenCases) {
        this.childrenCases = childrenCases;
    }
    
    /**
     * @return list of children cases (empty list if none)
     */
    public List<CaseID> getChildrenCases() {
        if (childrenCases == null) return Collections.emptyList();
        return childrenCases;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    /**
     * @return list of tags (empty list if none)
     */
    public List<String> getTags() {
        if (tags == null) return Collections.emptyList();
        return tags;
    }
    
    public CaseID getParentCase() {
        return parentCase;
    }
    
    public void setParentCase(CaseID parentCase) {
        this.parentCase = parentCase;
    }
}
