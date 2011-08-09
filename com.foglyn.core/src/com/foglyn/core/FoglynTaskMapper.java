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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;

import com.foglyn.fogbugz.FogBugzPriority.PriorityID;

public class FoglynTaskMapper extends TaskMapper {

    public FoglynTaskMapper(TaskData taskData) {
        super(taskData);
    }

    @Override
    public boolean applyTo(ITask task) {
        boolean changed = super.applyTo(task);
        
        String categoryID = getTaskData().getRoot().getAttribute(FoglynAttribute.CATEGORY_ID.getKey()).getValue();
        
        // Foglyn UI plugin needs following extra values
        if (categoryID != null && !categoryID.equals(task.getAttribute(FoglynAttribute.CATEGORY_ID.getKey()))) {
            task.setAttribute(FoglynConstants.TASK_ATTRIBUTE_CATEGORY, categoryID);
            changed = true;
        }
        
        TaskAttribute attribute = getTaskData().getRoot().getAttribute(FoglynAttribute.CURRENT_ESTIMATE_HOURS.getKey());
        if (attribute != null) {
            String estimate = attribute.getValue();
            BigDecimal bd = new BigDecimal(estimate);
            // round to integer
            bd = bd.setScale(0, RoundingMode.HALF_EVEN);
            
            int hours = bd.intValue();
            
            if (task instanceof AbstractTask) {
                ((AbstractTask) task).setEstimatedTimeHours(hours);
            }
        }
        
        TaskAttribute origEst = getTaskData().getRoot().getAttribute(FoglynAttribute.ORIGINAL_ESTIMATE_HOURS.getKey());
        if (origEst != null && isCaseOpen()) {
            task.setAttribute(FoglynConstants.TASK_ATTRIBUTE_TIME_TRACKING_ENABLED, "true");
        }

        // Remember last update date of fully updated task
        if (!getTaskData().isPartial() && getModificationDate() != null) {
            task.setAttribute(FoglynConstants.TASK_ATTRIBUTE_FULL_TASKDATA_LAST_UPDATE, Long.toString(getModificationDate().getTime()));
        }

        TaskAttribute subcasesSupported = getTaskData().getRoot().getAttribute(FoglynAttribute.REPOSITORY_IS_FOGBUGZ7_REPOSITORY.getKey());
        if (subcasesSupported != null) {
            // used in FoglynTaskDataHandler.canInitializeSubTaskData
            task.setAttribute(FoglynConstants.REPOSITORY_IS_FOGBUGZ7_REPOSITORY, subcasesSupported.getValue());
        }
        
        return changed;
    }
    
    @Override
    public Date getCompletionDate() {
        TaskRepository repos = getTaskData().getAttributeMapper().getTaskRepository();
        CompletedCaseMode mode = Utils.getCompletedCaseMode(repos);

        Date closedDate = getClosedDateIfClosed();
        Date resolvedDate = getResolvedDateIfResolved();
        
        boolean isAssignedToCurrentUser = isAssignedToCurrentUser();

        switch (mode) {
        case CLOSED:
            if (closedDate != null) {
                return closedDate;
            }
            
            return null;
        case RESOLVED_OR_CLOSED:
            if (closedDate != null) {
                return closedDate;
            }
            
            if (resolvedDate != null) {
                return resolvedDate;
            }
            
            return null;
        case SMART_RESOLVED_OR_CLOSED:
            if (closedDate != null) {
                return closedDate;
            }
            
            if (!isAssignedToCurrentUser && resolvedDate != null) {
                return resolvedDate;
            }
            
            return null;
        }
        
        return null;
    }

    private boolean isCaseOpen() {
        TaskAttributeMapper am = getTaskData().getAttributeMapper();
        TaskAttribute open = getTaskData().getRoot().getAttribute(FoglynAttribute.OPEN.getKey());

        return am.getBooleanValue(open);
    }
    
    private Date getClosedDateIfClosed() {
        if (isCaseOpen()) {
            return null;
        }
       
        TaskAttribute closedDate = getTaskData().getRoot().getAttribute(FoglynAttribute.CLOSED_DATE.getKey());
        if (closedDate != null) {
            TaskAttributeMapper am = getTaskData().getAttributeMapper();
            return am.getDateValue(closedDate);
        }
        
        return null;
    }

    private boolean isCaseResolved() {
        TaskAttribute resolved = getTaskData().getRoot().getAttribute(FoglynAttribute.STATUS_IS_RESOLVED.getKey());
        if (resolved != null) {
            TaskAttributeMapper am = getTaskData().getAttributeMapper();
            return am.getBooleanValue(resolved);
        }
        
        return false;
    }
    
    private Date getResolvedDateIfResolved() {
        if (!isCaseResolved()) {
            return null;
        }

        TaskAttribute closedDate = getTaskData().getRoot().getAttribute(FoglynAttribute.RESOLVED_DATE.getKey());
        if (closedDate != null) {
            TaskAttributeMapper am = getTaskData().getAttributeMapper();
            return am.getDateValue(closedDate);
        }
        
        return null;
    }
    
    private boolean isAssignedToCurrentUser() {
        TaskAttribute assignedToCurrentUser = getTaskData().getRoot().getAttribute(FoglynAttribute.ASSIGNED_TO_CURRENT_USER.getKey());
        if (assignedToCurrentUser != null) {
            TaskAttributeMapper am = getTaskData().getAttributeMapper();
            return am.getBooleanValue(assignedToCurrentUser);
        }
        
        return false;
    }
    
    @Override
    public PriorityLevel getPriorityLevel() {
        String prio = getTaskData().getRoot().getAttribute(FoglynAttribute.PRIORITY_ID.getKey()).getValue();

        PriorityID prioID = PriorityID.valueOf(prio);
        
        // map small values to big priority...
        if ("1".equals(prioID.toString())) {
            return PriorityLevel.P1;
        }
        if ("2".equals(prioID.toString())) {
            return PriorityLevel.P2;
        }
        if ("3".equals(prioID.toString())) {
            return PriorityLevel.P3;
        }
        if ("4".equals(prioID.toString())) {
            return PriorityLevel.P4;
        }
        
        return PriorityLevel.P5;
    }
    
    @Override
    public void setDescription(String description) {
        setValue(FoglynAttribute.MYLYN_NEW_COMMENT.getKey(), description);
    }
}
