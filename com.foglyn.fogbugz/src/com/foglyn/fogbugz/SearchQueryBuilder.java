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

import java.util.ArrayList;
import java.util.List;

import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzCase.CaseID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;

/**
 * http://www.fogcreek.com/fogbugz/docs/60/topics/basics/Searchingforcases.html
 * http://www.fogcreek.com/fogbugz/docs/70/topics/basics/Searchingforcases.html
 */
public class SearchQueryBuilder {
    private final FogBugzClient client;
    private final List<String> axes;
    private String freeFormQuery;
    
    public SearchQueryBuilder(FogBugzClient client) {
        this.axes = new ArrayList<String>();
        this.client = client;
        this.freeFormQuery = null;
    }

    private String axisString(String name, String value) {
        return String.format("%s:\"%s\"", name, value);
    }
    
    private String axisIndex(String name, ID value) {
        return String.format("%s:=%s", name, value.toString());
    }
    
    /**
     * @param axisName
     * @param personID
     * @throws IllegalArgumentException if personID doesn't correspond to any person, known by this client
     */
    private String personAxis(String axisName, PersonID personID) {
        FogBugzPerson person = client.getPerson(personID);
        
        if (person == null) {
            throw new IllegalArgumentException("Unknown person with index: " + personID);
        }
        
        return axisString(axisName, person.getFullName());
    }

    /**
     * Cases due on the date specified
     */
    public void due(String date) {
        axes.add(axisString("Due", date));
    }

    /**
     * Cases modified on the date specified
     */
    public void edited(String date) {
        axes.add(axisString("Edited", date));
    }
    
    public void editedBy(PersonID person) {
        axes.add(personAxis("EditedBy", person));
    }

    /**
     * cases edited by the specified user, to be used in combination with EditedBy
     */
    public void alsoEditedBy(PersonID alsoEditedBy) {
        axes.add(personAxis("AlsoEditedBy", alsoEditedBy));
    }
    
    public void createdBy(PersonID person) {
        axes.add(personAxis("CreatedBy", person));
    }
    
    /**
     * Cases last closed by the specified user
     */
    public void closedBy(PersonID person) {
        axes.add(personAxis("ClosedBy", person));
    }
    
    public void closed(String date) {
        axes.add(axisString("Closed", date));
    }
    
    /**
     * Cases assigned to the specified user
     */
    public void assignedTo(PersonID person) {
        axes.add(personAxis("AssignedTo", person));
    }
    
    public void assignedToMe() {
        axes.add(axisString("AssignedTo", "me"));
    }
    
    /**
     * Cases with an attachment with the specified name
     */
    public void attachment(String filename) {
        axes.add(axisString("Attachment", filename));
    }
    
    /**
     * Cases with the specified email correspondent
     */
    public void correspondent(String email) {
        axes.add(axisString("Correspondent", email));
    }
    
    /**
     * Cases in the specified area
     */
    public void area(AreaID area) {
        axes.add(axisIndex("Area", area));
    }

    /**
     * Cases with the specified category
     */
    public void category(CategoryID categoryID) {
        axes.add(axisIndex("Category", categoryID));
    }

    /**
     * Cases assigned to be fixed for the specified release
     */
    public void fixFor(final FixForID fixForID) {
        axes.add(axisIndex("FixFor", fixForID));
    }
    
    /**
     * Cases with emails from the specified email address
     */
    public void from(String email) {
        axes.add(axisString("From", email));
    }
    
    /**
     * Cases that were modified on the date specified and have not been modified since then
     */
    public void lastEdited(String date) {
        axes.add(axisString("LastEdited", date));
    }
    
    /**
     * Cases last edited by the specified user
     */
    public void lastEditedBy(PersonID person) {
        axes.add(personAxis("LastEditedBy", person));
    }

    /**
     * Cases opened on the date specified
     */
    public void opened(String date) {
        axes.add(axisString("Opened", date));
    }
    
    /**
     * Cases last opened or reopened by the specified user
     */
    public void openedBy(PersonID person) {
        axes.add(personAxis("OpenedBy", person));
    }

    /**
     * Cases with the specified priority
     */
    public void priority(PriorityID priority) {
        axes.add(axisIndex("Priority", priority));
    }
    
    /**
     * Cases in the specified project
     */
    public void project(ProjectID projectID) {
        axes.add(axisIndex("Project", projectID));
    }
    
    /**
     * Cases that are linked to the specified case
     */
    public void relatedTo(CaseID caseID) {
        axes.add(axisIndex("RelatedTo", caseID));
    }
    
    /**
     * Cases resolved on the date specified
     */
    public void resolved(String date) {
        axes.add(axisString("Resolved", date));
    }

    /**
     * Cases last resolved by the specified user
     */
    public void resolvedBy(PersonID person) {
        axes.add(personAxis("ResolvedBy", person));
    }

    /**
     * Show subscribed cases
     */
    public void subscribed() {
        axes.add("Show:subscribed");
    }
    
    public void starredByMe() {
        axes.add("StarredBy:me");
    }

    /**
     * Cases with the specified status
     */
    public void status(StatusID status) {
        axes.add(axisIndex("Status", status));
    }

    public void status(StatusID... statusIDs) {
        if (statusIDs == null || statusIDs.length == 0) {
            return;
        }
        
        List<String> conds = new ArrayList<String>();
        for (StatusID s: statusIDs) {
            conds.add(axisIndex("Status", s));
        }
        
        axes.add(or(conds));
    }
    
    /**
     * Cases containing the specified words in the title
     */
    public void title(String title) {
        axes.add(axisString("Title", title));
    }
    
    /**
     * Cases with email to the specified email address
     */
    public void emailedTo(String email) {
        axes.add(axisString("To", email));
    }
    
    public void setFreeFormQuery(String freeFormQuery) {
        this.freeFormQuery = freeFormQuery;
    }
    
    public void tags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        for (String t: tags) {
            axes.add(axisString("Tag", t));
        }
    }

    private static String or(List<String> conditions) {
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        
        String delim = "";
        for (String c: conditions) {
            sb.append(delim);
            sb.append(c);
            
            delim=" OR ";
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    public String buildQuery() {
        StringBuilder result = new StringBuilder();
        
        if (!axes.isEmpty()) {
            result.append("(");
            
            boolean first = true;
            for (String a: axes) {
                if (!first) result.append(" ");
                first = false;
                result.append(a);
            }
            
            result.append(")");
        }

        if (freeFormQuery != null) {
            if (!axes.isEmpty()) {
                result.append(" ");
            }
            
            result.append(freeFormQuery);
        }
        
        return result.toString();
    }
}
