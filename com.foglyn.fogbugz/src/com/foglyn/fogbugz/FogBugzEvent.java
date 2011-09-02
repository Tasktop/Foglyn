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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.foglyn.fogbugz.FogBugzPerson.PersonID;

/**
 * Represents event of case, i.e. what happened to case.
 */
public class FogBugzEvent {
    public final static class EventID extends LongID {
        private EventID(long caseID) {
            super(caseID);
        }

        public static EventID valueOf(String ixBugEvent) {
            return new EventID(Long.parseLong(ixBugEvent));
        }
    }

    private EventID eventID;
    private String verb; // description in english, not very useful sometimes :-(
    private PersonID initiator;
    private Date date;
    private String text;
    private String changes;
    
    private String eventDescription;

    private boolean email;
    private String emailFrom;
    
    private List<FogBugzAttachment> attachments;

    FogBugzEvent() {
        //
    }

    /**
     * @return date when event happened
     */
    public Date getDate() {
        return Utils.copyOf(date);
    }

    public EventID getEventID() {
        return eventID;
    }

    /**
     * @return description of changes to case during this event. E.g. "Project changed from 'Inbox' to 'Cave'.".
     */
    public String getChanges() {
        return changes;
    }

    /**
     * @return ID of person who initiated this event.
     */
    public PersonID getInitiator() {
        return initiator;
    }

    /**
     * @return text of event (usually from user). If this event is an email,
     *         this method returns simple version of email (including headers, and body).
     */
    public String getText() {
        return text;
    }

    /**
     * @return Description of event (in English always). Example: "Assigned to Captain Caveman", "Edited"..
     */
    public String getVerb() {
        return verb;
    }

    /**
     * @return Event description, in users-language. This is more complete than verb, contains also 'initiator'
     */
    public String getEventDescription() {
        return eventDescription;
    }
    
    public void setDate(Date date) {
        this.date = Utils.copyOf(date);
    }

    public void setEventID(EventID eventID) {
        this.eventID = eventID;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public void setInitiator(PersonID initiator) {
        this.initiator = initiator;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }
    
    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public void setAttachments(List<FogBugzAttachment> attachments) {
        this.attachments = new ArrayList<FogBugzAttachment>(attachments);
    }
    
    public List<FogBugzAttachment> getAttachments() {
        if (attachments == null) {
            return Collections.emptyList();
        }
		return Collections.unmodifiableList(attachments);
    }
    
    @Override
    public String toString() {
        return "Event: " + eventDescription + " (" + eventID + ")";
    }

    public void setEmail(boolean isEmail) {
        this.email = isEmail;
    }
    
    public boolean isEmail() {
        return email;
    }
    
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }
    
    public String getEmailFrom() {
        return emailFrom;
    }
}
