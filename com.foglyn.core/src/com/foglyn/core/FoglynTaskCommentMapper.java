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

package com.foglyn.core;

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;

public class FoglynTaskCommentMapper extends TaskCommentMapper {
    private TaskAttribute commentAttribute; // non-null, when created via createFrom method
    
    private String verb;
    private String changes;
    private String eventDescription;

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getEventDescription() {
        return eventDescription;
    }
    
    public void setCommentAttribute(TaskAttribute commentAttribute) {
        this.commentAttribute = commentAttribute;
    }
    
    public TaskAttribute getCommentAttribute() {
        return commentAttribute;
    }

    public static FoglynTaskCommentMapper createFrom(TaskAttribute commentAttribute) {
        Assert.isNotNull(commentAttribute);
        TaskData taskData = commentAttribute.getTaskData();
        TaskAttributeMapper mapper = taskData.getAttributeMapper();
        
        FoglynTaskCommentMapper comment = new FoglynTaskCommentMapper();
        comment.commentAttribute = commentAttribute;
        
        comment.setCommentId(mapper.getValue(commentAttribute));
        TaskAttribute child = commentAttribute.getMappedAttribute(TaskAttribute.COMMENT_AUTHOR);
        if (child != null) {
            IRepositoryPerson person = mapper.getRepositoryPerson(child);
            comment.setAuthor(person);
        }
        child = commentAttribute.getMappedAttribute(TaskAttribute.COMMENT_DATE);
        if (child != null) {
            comment.setCreationDate(mapper.getDateValue(child));
        }
        child = commentAttribute.getMappedAttribute(TaskAttribute.COMMENT_NUMBER);
        if (child != null) {
            comment.setNumber(mapper.getIntegerValue(child));
        }
        child = commentAttribute.getMappedAttribute(TaskAttribute.COMMENT_URL);
        if (child != null) {
            comment.setUrl(mapper.getValue(child));
        }
        child = commentAttribute.getMappedAttribute(TaskAttribute.COMMENT_TEXT);
        if (child != null) {
            comment.setText(mapper.getValue(child));
        }
        
        // foglyn specific
        child = commentAttribute.getAttribute(FoglynAttribute.EVENT_VERB.getKey());
        if (child != null) {
            comment.setVerb(mapper.getValue(child));
        }
        
        child = commentAttribute.getAttribute(FoglynAttribute.EVENT_CHANGES.getKey());
        if (child != null) {
            comment.setChanges(mapper.getValue(child));
        }

        child = commentAttribute.getAttribute(FoglynAttribute.EVENT_DESCRIPTION.getKey());
        if (child != null) {
            comment.setEventDescription(mapper.getValue(child));
        }
        return comment;
    }
    
    @Override
    public void applyTo(TaskAttribute taskAttribute) {
        super.applyTo(taskAttribute);

        if (getVerb() != null) {
            Utils.createAttribute(taskAttribute, FoglynAttribute.EVENT_VERB).setValue(getVerb());
        }
        
        if (getChanges() != null) {
            Utils.createAttribute(taskAttribute, FoglynAttribute.EVENT_CHANGES).setValue(getChanges());
        }
        
        if (getEventDescription() != null) {
            Utils.createAttribute(taskAttribute, FoglynAttribute.EVENT_DESCRIPTION).setValue(getEventDescription());
        }
    }

    public boolean hasChanges() {
        return Utils.nonEmpty(getChanges());
    }
    
    public boolean hasText() {
        return Utils.nonEmpty(getText());
    }
}
