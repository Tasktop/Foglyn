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

package com.foglyn.core;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

/**
 * Task attributes stored for offline usage.
 */
public enum FoglynAttribute {
    // pretty name, key, type, visible, readonly
    
    // case number
    BUG_ID("Case:", "fb_ixBug", TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    LATEST_EVENT_ID("Latest event ID:", "fb_ixBugEventLatest", TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    // true if open, false if closed 
    OPEN("Open:", "fb_fOpen", TaskAttribute.TYPE_BOOLEAN, false, true),

    TITLE("Title:", "fb_sTitle", TaskAttribute.TYPE_SHORT_RICH_TEXT, false, false),
    
    BUG_LATEST_TEXT_EVENT("fb_ixBugEventLatestText", "fb_ixBugEventLatestText", TaskAttribute.TYPE_INTEGER, false, true),
    
    PROJECT_ID("Project:", "fb_ixProject", FoglynConstants.TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES, true, false),
    
    AREA_ID("Area:", "fb_ixArea", FoglynConstants.TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES, true, false),
    
    // GROUP_ID("Group ID:", "fb_ixGroup", TaskAttribute.TYPE_INTEGER, false, true),
    
    ASSIGNED_TO_PERSON_ID("Assigned to:", "fb_ixPersonAssignedTo", FoglynConstants.TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES, false, false),
    
    ASSIGNED_TO_CURRENT_USER("Assigned to Current User:", "foglyn_assignedToCurrentUser", TaskAttribute.TYPE_BOOLEAN, false, true),

    OPENED_BY_PERSON_ID("Opened by:", "fb_ixPersonOpenedBy", TaskAttribute.TYPE_SHORT_TEXT, false, true),

    LAST_UPDATED_DATE("Last Updated:", "fb_dtLastUpdated", TaskAttribute.TYPE_DATE, false, true),
    
    STATUS_ID("Status:", "fb_ixStatus", TaskAttribute.TYPE_SINGLE_SELECT, false, false),
    
    STATUS_IS_RESOLVED("Resolved:", "foglyn_resolvedStatus", TaskAttribute.TYPE_BOOLEAN, false, true),
    
    EDITABLE_ACTIVE_STATUS_ID("Status:", "foglyn_editable_active_status", FoglynConstants.TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES, false, false),
    
    // Editable status is used to display only statuses for resolving (not Active). Value is based on default value for category.
    EDITABLE_RESOLVED_STATUS_ID("Status:", "foglyn_editable_status", FoglynConstants.TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES, false, false),

    PRIORITY_ID("Priority:", "fb_ixPriority", TaskAttribute.TYPE_SINGLE_SELECT, true, false),

    // Since FogBugz 7, this is now "Milestone:"
    FIX_FOR("Fix For:", "fb_ixFixFor", FoglynConstants.TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES, true, false),

    CATEGORY_ID("Category:", "fb_ixCategory", TaskAttribute.TYPE_SINGLE_SELECT, true, false),
    
    OPENED_DATE("Opened Date:", "fb_dtOpened", TaskAttribute.TYPE_DATE, false, false),
    
    RESOLVED_DATE("Resolved Date:", "fb_dtResolved", TaskAttribute.TYPE_DATE, false, false),
    
    CLOSED_DATE("Closed Date:", "fb_dtClosed", TaskAttribute.TYPE_DATE, false, false),

    DUE_DATE("Due Date:", "fb_dtDue", FoglynConstants.TYPE_CUSTOM_DATETIME, true, false),

    EVENT_VERB("", "fb_sVerb", TaskAttribute.TYPE_SHORT_TEXT, false, true),

    EVENT_CHANGES("", "fb_sChanges", TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    EVENT_DESCRIPTION("", "fb_evtDescription", TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    ATTACHMENT_URL_COMPONENT("", "fb_sURL", TaskAttribute.TYPE_URL, false, true),
    
    ORIGINAL_ESTIMATE_HOURS("Original Estimate:", "fb_hrsOrigEst", TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    // DHM = days/hours/minutes format
    ORIGINAL_ESTIMATE_DHM("Original Estimate:", "foglyn_hrsOrigEst_dhm", FoglynConstants.TYPE_DAYS_HOURS_MINUTES, true, true),
    
    CURRENT_ESTIMATE_HOURS("Current Estimate:", "fb_hrsCurrEst", TaskAttribute.TYPE_SHORT_TEXT, false, true),

    CURRENT_ESTIMATE_DHM("Current Estimate:", "foglyn_hrsCurrEst_dhm", FoglynConstants.TYPE_DAYS_HOURS_MINUTES, true, false),
    
    ELAPSED_TIME_HOURS("Elapsed Time:", "fb_hrsElapsed", TaskAttribute.TYPE_SHORT_TEXT, false, true),

    // Elapsed time cannot be modified at the moment via FogBugz API
    ELAPSED_TIME_DHM("Elapsed Time:", "foglyn_hrsElapsed_dhm", FoglynConstants.TYPE_DAYS_HOURS_MINUTES, true, true),
    
    REMAINING_TIME_HOURS("Remaining Time:", "foglyn_remainingTime_hours", TaskAttribute.TYPE_SHORT_TEXT, false, true),

    REMAINING_TIME_DHM("Remaining Time:", "foglyn_remainingTime_dhm", FoglynConstants.TYPE_DAYS_HOURS_MINUTES, true, true),
    
    RELATED_CASES("Related Cases:", "foglyn_relatedCases", TaskAttribute.TYPE_TASK_DEPENDENCY, true, true),

    REPOSITORY_IS_FOGBUGZ7_REPOSITORY("", "foglyn_isFogBugz7Repository", TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    PARENT_CASE("Parent Case:", "fb_ixBugParent", TaskAttribute.TYPE_TASK_DEPENDENCY, true, false),

    CHILDREN_CASES("Children Cases:", "fb_ixBugChildren", TaskAttribute.TYPE_TASK_DEPENDENCY, true, true),
    
    TAGS("Tags:", "fb_tags", TaskAttribute.TYPE_SHORT_TEXT, true, false),

    // Mylyn internal attributes: aren't mapped in FoglynTaskAttributeMapper, but we keep them here to ease attribute creation.
    // These values are computed from case data, cannot be modified, and usually isn't displayed to user in editor. These values are used by Mylyn.
    MYLYN_STATUS("Status:", TaskAttribute.STATUS, TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    /**
     * Task URL. This is Mylyn's internal task attribute.
     */
    MYLYN_URL("URL:", TaskAttribute.TASK_URL, TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    /**
     * Task priority. This is Mylyn's internal task attribute.
     */
    MYLYN_PRIORITY("Priority", TaskAttribute.PRIORITY, TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    /**
     * "Opened by". This is Mylyn's internal task attribute.
     */
    MYLYN_REPORTER("Opened by:", TaskAttribute.USER_REPORTER, TaskAttribute.TYPE_SHORT_TEXT, false, true),
    
    /**
     * User-entered comment. This is Mylyn's internal task attribute. Connector sends this to server.
     */
    MYLYN_NEW_COMMENT("New Comment:", TaskAttribute.COMMENT_NEW, TaskAttribute.TYPE_LONG_RICH_TEXT, false, false),
    
    /**
     * Operation. This is Mylyn's internal task attribute.
     */
    MYLYN_OPERATION("Operation:", TaskAttribute.OPERATION, TaskAttribute.TYPE_OPERATION, false, false);

    private final boolean isVisible;

    private final boolean isReadOnly;

    private final String keyString;

    private final String prettyName;

    private final String type;
    
    FoglynAttribute(String prettyName, String idKey, String type, boolean visible, boolean readonly) {
        this.prettyName = prettyName;
        this.keyString = idKey;
        this.type = type;
        this.isVisible = visible;
        this.isReadOnly = readonly;
    }

    /**
     * @return key (identifier) of attribute. Usually named after FogBugz field name, but can be any arbitrary name.
     */
    public String getKey() {
        return keyString;
    }

    /**
     * @return Is this attribute editable or read only?
     */
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * "Pretty name" of attribute.
     */
    public String toString() {
        return prettyName;
    }

    /**
     * @return {@link TaskAttribute#KIND_DEFAULT} if task is visible, <code>null</code> otherwise
     */
    public String getKind() {
        return isVisible ? TaskAttribute.KIND_DEFAULT : null;
    }

    /**
     * @return type of attribute, usually some TYPE_ constants from {@link TaskAttribute} class, or custom type.
     */
    public String getType() {
        return type;
    }
}
