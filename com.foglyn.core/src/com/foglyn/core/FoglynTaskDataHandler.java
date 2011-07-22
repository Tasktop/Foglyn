package com.foglyn.core;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;

import com.foglyn.core.FoglynConstants.Dependency;
import com.foglyn.fogbugz.CaseAction;
import com.foglyn.fogbugz.ChangeEventData;
import com.foglyn.fogbugz.DaysHoursMinutes;
import com.foglyn.fogbugz.FogBugzArea;
import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzAttachment;
import com.foglyn.fogbugz.FogBugzCase;
import com.foglyn.fogbugz.FogBugzCase.CaseID;
import com.foglyn.fogbugz.FogBugzCategory;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzEvent;
import com.foglyn.fogbugz.FogBugzEvent.EventID;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzFixFor;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzPerson;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPriority;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzProject;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzResponseCaseHasBeenChangedException;
import com.foglyn.fogbugz.FogBugzStatus;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;
import com.foglyn.fogbugz.HasID;
import com.foglyn.fogbugz.ID;

public class FoglynTaskDataHandler extends AbstractTaskDataHandler {
    private static final PriorityID DEFAULT_PRIORITY = PriorityID.valueOf("3");
    private static final PersonID CLOSED_PERSON_ID = PersonID.valueOf("1");
    
    private final FoglynRepositoryConnector connector;
    private final RepositoryDataManager repositoryDataManager;
    
    FoglynTaskDataHandler(FoglynRepositoryConnector connector, RepositoryDataManager repositoryDataManager) {
        this.connector = connector;
        this.repositoryDataManager = repositoryDataManager;
    }

    @Override
    public FoglynTaskAttributeMapper getAttributeMapper(TaskRepository taskRepository) {
        return new FoglynTaskAttributeMapper(taskRepository);
    }

    @Override
    public boolean initializeTaskData(TaskRepository repository, TaskData data, ITaskMapping initializationData, IProgressMonitor monitor)
            throws CoreException {
        FogBugzClient client = connector.getClientManager().getFogbugzClient(repository, monitor);
        
        createEmptyTaskData(data, repository, client, null);
        
        return true;
    }

    @Override
    public boolean initializeSubTaskData(TaskRepository repository, TaskData data, TaskData parentTaskData, IProgressMonitor monitor)
            throws CoreException {
        if (!Utils.isFogBugz7Repository(repository, null, parentTaskData)) {
            return false;
        }
        
        if (parentTaskData == null) {
            return false;
        }
        
        
        FogBugzClient client = connector.getClientManager().getFogbugzClient(repository, monitor);

        CaseID parentCaseID = CaseID.valueOf(parentTaskData.getTaskId());
        
        createEmptyTaskData(data, repository, client, parentCaseID);
        
        return true;
    }

    @Override
    public boolean canInitializeSubTaskData(TaskRepository taskRepository, ITask task) {
        return Utils.isFogBugz7Repository(taskRepository, task, null);
    }
    
    @Override
    public RepositoryResponse postTaskData(TaskRepository repository,
            TaskData taskData, Set<TaskAttribute> oldAttributes, IProgressMonitor monitor) throws CoreException {

        FogBugzClient client = connector.getClientManager().getFogbugzClient(repository, monitor);
        
        CaseAction action = null;
        ChangeEventData cdata = new ChangeEventData();
        
        FoglynAttribute statusAttribute = null;
        if (taskData.isNew()) {
            action = CaseAction.NEW;
        } else {
            String actionString = getAttributeValue(taskData, FoglynAttribute.MYLYN_OPERATION);
            action = CaseAction.valueOf(actionString);

            cdata.setCaseID(CaseID.valueOf(getAttributeValue(taskData, FoglynAttribute.BUG_ID)));
            cdata.setEventID(getLatestEventID(taskData));
            
            TaskAttribute operationAttribute = taskData.getRoot().getAttribute(TaskAttribute.PREFIX_OPERATION + actionString);
            if (operationAttribute != null) {
                if (FoglynAttribute.EDITABLE_ACTIVE_STATUS_ID.getKey().equals(operationAttribute.getMetaData().getValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID))) {
                    statusAttribute = FoglynAttribute.EDITABLE_ACTIVE_STATUS_ID;
                }
                
                if (FoglynAttribute.EDITABLE_RESOLVED_STATUS_ID.getKey().equals(operationAttribute.getMetaData().getValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID))) {
                    statusAttribute = FoglynAttribute.EDITABLE_RESOLVED_STATUS_ID;
                }
            }
        }
        
        cdata.setNewComment(getAttributeValue(taskData, FoglynAttribute.MYLYN_NEW_COMMENT));
        cdata.setNewTitle(getAttributeValue(taskData, FoglynAttribute.TITLE));
        cdata.setNewProjectID(ProjectID.valueOf(getAttributeValue(taskData, FoglynAttribute.PROJECT_ID)));
        cdata.setNewAreaID(AreaID.valueOf(getAttributeValue(taskData, FoglynAttribute.AREA_ID)));
        cdata.setNewFixForID(FixForID.valueOf(getAttributeValue(taskData, FoglynAttribute.FIX_FOR)));
        cdata.setNewCategoryID(CategoryID.valueOf(getAttributeValue(taskData, FoglynAttribute.CATEGORY_ID)));
        cdata.setNewAssignedTo(PersonID.valueOf(getAttributeValue(taskData, FoglynAttribute.ASSIGNED_TO_PERSON_ID)));
        cdata.setNewPriorityID(PriorityID.valueOf(getAttributeValue(taskData, FoglynAttribute.PRIORITY_ID)));

        // these attributes may not be available if user/site schedule is not available
        cdata.setNewDueDate(getDateAttributeValue(taskData, FoglynAttribute.DUE_DATE));
        cdata.setNewCurrentEstimate(getDaysHoursMinutesFromAttribute(taskData, FoglynAttribute.CURRENT_ESTIMATE_DHM));
        
        // elapsed time cannot be modified at the moment via FogBugz API
        // cdata.setNewElapsedTime(getDaysHoursMinutesFromAttribute(taskData, FoglynAttribute.ELAPSED_TIME_DHM));
        
        // make sure this taskData knows about subcases ... otherwise we might overwrite parent
        if (Utils.isFogBugz7Repository(null, null, taskData)) {
            TaskAttribute attr = taskData.getRoot().getAttribute(FoglynAttribute.PARENT_CASE.getKey());
            if (attr != null) {
                List<CaseID> caseIDs = Utils.getCaseIDsFromString(attr.getValues());
                if (caseIDs.size() == 1) {
                    cdata.setParentCaseID(caseIDs.get(0));
                }
            }
            
            TaskAttribute tagsAttr = taskData.getRoot().getAttribute(FoglynAttribute.TAGS.getKey());
            if (tagsAttr != null) {
                cdata.setTags(Utils.getTagsFromString(tagsAttr.getValue()));
            }
        }

        // Assign to owner if user hasn't selected person
        if (action == CaseAction.REOPEN && CLOSED_PERSON_ID.equals(cdata.getNewAssignedTo())) {
            cdata.setNewAssignedTo(client.getOwner(cdata.getNewProjectID(), cdata.getNewAreaID()));
        }

        if (statusAttribute != null) {
            String activeStatus =  getOptionalAttributeValue(taskData, statusAttribute);
            if (activeStatus != null) {
                cdata.setNewStatus(StatusID.valueOf(activeStatus));
            }
        }
        
        FogBugzCase result = null;
        try {
            result = client.performCaseAction(action, cdata, monitor, false);
            
            if (cdata.getNewCurrentHoursEstimate() != null && client.getWorkingSchedule() != null) {
                DaysHoursMinutes sent = cdata.getNewCurrentHoursEstimate();
                sent = sent.normalize(client.getWorkingSchedule().getWorkingHoursPerDay());
                
                DaysHoursMinutes received = result.getConvertedCurrentEstimate();
                
                if (!received.equals(sent)) {
                    String server = repository.getUrl();
                    
                    FoglynCorePlugin.getDefault().log(Status.WARNING, "" + server + ": incorrect 'current estimate' received. " +
                    		"Sent: " + sent + ", received: " + received + ". Case: " + result.getCaseID());
                }
            }
        } catch (FogBugzResponseCaseHasBeenChangedException e) {
            throw new CoreException(Utils.toErrorStatus("This case has been changed since you last viewed it. Please synchronize incoming changes before submitting again.", e));
        } catch (FogBugzException e) {
            StatusHandler.log(Utils.toStatus(e));
            
            throw new FoglynCoreException(e);
        }
        
        if (taskData.isNew()) {
            // Save default values for new case
            DefaultCaseData dcd = new DefaultCaseData();
            dcd.setDefaultProject(cdata.getNewProjectID());
            dcd.setDefaultArea(cdata.getNewAreaID());
            dcd.setDefaultCategory(cdata.getNewCategoryID());
            dcd.setDefaultOwner(cdata.getNewAssignedTo());
            dcd.setDefaultPriority(cdata.getNewPriorityID());
            dcd.setDefaultFixFor(cdata.getNewFixForID());
            
            repositoryDataManager.saveDefaultCaseData(repository, dcd);
            
            return new RepositoryResponse(ResponseKind.TASK_CREATED, result.getCaseID().toString());
        } else {
            return new RepositoryResponse(ResponseKind.TASK_UPDATED, result.getCaseID().toString());
        }
    }

    /**
     * @return days/hours/minutes value from attribute if set. If attribute is not set, then returned value
     * depends on whether original estimate is set... if so, 0 is returned. It not, null is returned (no change). 
     */
    private DaysHoursMinutes getDaysHoursMinutesFromAttribute(TaskData taskData, FoglynAttribute attr) {
        TaskAttribute attribute = taskData.getRoot().getAttribute(attr.getKey());
        if (attribute == null) {
            return null;
        }
        
        String estimate = attribute.getValue();
        if (estimate != null && estimate.trim().length() > 0) {
            return DaysHoursMinutes.parseDaysHoursMinutesSlashForm(estimate);
        }
        
        return null;
    }

    private EventID getLatestEventID(TaskData taskData) {
        TaskAttribute attr = taskData.getRoot().getAttribute(FoglynAttribute.LATEST_EVENT_ID.getKey());
        if (attr != null) {
            return EventID.valueOf(attr.getValue());
        } else {
            return null;
        }
    }
    
    private String getAttributeValue(TaskData data, FoglynAttribute attribute) {
        return data.getRoot().getAttribute(attribute.getKey()).getValue();
    }

    private String getOptionalAttributeValue(TaskData data, FoglynAttribute attribute) {
        TaskAttribute attr = data.getRoot().getAttribute(attribute.getKey());
        if (attr != null) {
            return attr.getValue();
        }
        
        return null;
    }
    
    private Date getDateAttributeValue(TaskData data, FoglynAttribute attribute) {
        TaskAttribute a = data.getRoot().getAttribute(attribute.getKey());
        if (a == null) {
            return null;
        }
        
        String s = a.getValue();
        if (s != null && s.trim().length() > 0) {
            long time = Long.parseLong(s);
            return new Date(time);
        }
        
        return null;
    }
    
    void createEmptyTaskData(TaskData data, TaskRepository repository, FogBugzClient client, CaseID parentCaseID) {
        FoglynTaskAttributeMapper mapper = getAttributeMapper(repository);

        DefaultCaseData defaultData = repositoryDataManager.getDefaultCaseData(repository);
        
        FogBugzCase fbCase = new FogBugzCase();
        fbCase.setOpen(true);
        fbCase.setTitle("");

        FogBugzProject project = getDefaultProject(defaultData.getDefaultProject(), client);
        FogBugzArea area = getDefaultArea(defaultData.getDefaultArea(), client, project);
        
        fbCase.setProject(getID(project));
        fbCase.setArea(getID(area));
        fbCase.setAssignedTo(getDefaultOwner(defaultData.getDefaultOwner(), project, area));

        CategoryID cid = defaultData.getDefaultCategory();
        if (cid == null) {
            cid = CategoryID.INQUIRY;
        }
        fbCase.setCategory(cid);
        
        fbCase.setStatus(getDefaultNewStatusForCategory(client, cid));

        PriorityID pid = defaultData.getDefaultPriority();
        if (pid == null) {
            pid = DEFAULT_PRIORITY;
        }
        fbCase.setPriority(pid);
        
        fbCase.setFixFor(getDefaultFixFor(defaultData.getDefaultFixFor(), client, project));
        
        fbCase.setOpenedDate(new Date());
        
        fbCase.setParentCase(parentCaseID);
        
        fillTaskDataFromFogBugzCase(fbCase, data, mapper, repository, client, false);
    }

    private StatusID getDefaultNewStatusForCategory(FogBugzClient client, CategoryID cid) {
        FogBugzCategory category = client.getCategory(cid);
        if (category != null) {
            StatusID defaultActive = category.getDefaultActiveStatus();
            if (defaultActive != null) {
                return defaultActive;
            }
        }
        
        Collection<FogBugzStatus> statuses = client.getAllStatuses();
        
        for (FogBugzStatus s: statuses) {
            if (s.isResolved()) continue;
            if (s.getCategoryID() != null && !cid.equals(s.getCategoryID())) {
                continue;
            }
            
            return s.getID();
        }
        
        // this used to be ACTIVE case in FogBugz 6.
        // should not happen
        return StatusID.valueOf("1");
    }

    /**
     * @return default FixFor for new task
     */
    private FixForID getDefaultFixFor(FixForID fixForID, FogBugzClient client, FogBugzProject project) {
        if (fixForID != null) {
            FogBugzFixFor ff = client.getFixFor(fixForID);
            if (ff != null && !ff.isDeleted() && (ff.getProject() == null || ff.getProject().equals(project.getID()))) {
                return ff.getID();
            }
        }
        
        Collection<FogBugzFixFor> fixFors = client.getAllFixFors();
        
        List<FogBugzFixFor> fixForsForProject = new ArrayList<FogBugzFixFor>();
        
        for (FogBugzFixFor ff: fixFors) {
            // Don't preselect unassignable FixFor
            if (ff.isDeleted()) {
                continue;
            }
            
            if (ff.getProject() == null) {
                fixForsForProject.add(ff);
            }
            
            if (project != null && ff.getProject() != null && ff.getProject().equals(project.getID())) {
                fixForsForProject.add(ff);
            }
        }
        
        if (!fixForsForProject.isEmpty()) {
            return fixForsForProject.get(0).getID();
        }
        
        // should not happen, but may if user deletes Undecided fix for
        return null;
    }

    private PersonID getDefaultOwner(PersonID personID, FogBugzProject project, FogBugzArea area) {
        if (personID != null) {
            return personID;
        }
        
        PersonID defaultOwner = personID;
        if (area != null && area.getOwner() != null) {
            defaultOwner = area.getOwner();
        } else if (project != null && project.getOwner() != null) {
            defaultOwner = project.getOwner();
        }
        return defaultOwner;
    }

    private <T extends ID> T getID(HasID<T> objectWithID) {
        if (objectWithID != null) {
            return objectWithID.getID();
        }
        
        return null;
    }

    private FogBugzArea getDefaultArea(AreaID defaultAreaID, FogBugzClient client, FogBugzProject project) {
        if (defaultAreaID != null) {
            FogBugzArea area = client.getArea(defaultAreaID);
            if (area != null && area.getProject().equals(project.getID())) {
                return area;
            }
        }

        // Find first area for this project
        Collection<FogBugzArea> allAreas = client.getAllAreas();
        for (FogBugzArea a: allAreas) {
            if (project != null && a.getProject().equals(project.getID())) {
                return a;
            }
        }
        
        return null;
    }

    private void setAttributeID(TaskAttributeMapper mapper, TaskAttribute attr, ID value) {
        if (value != null) {
            mapper.setValue(attr, value.toString());
        }
    }

    private FogBugzProject getDefaultProject(ProjectID projectID, FogBugzClient client) {
        if (projectID != null) {
            FogBugzProject project = client.getProject(projectID);
            if (project != null) {
                return project;
            }
        }
        
        Collection<FogBugzProject> allProjects = client.getAllProjects();
        if (!allProjects.isEmpty()) {
            return allProjects.iterator().next();
        }
        
        return null;
    }
    
    TaskData createTaskData(TaskRepository repository, FogBugzCase fbCase, FogBugzClient client) {
        FoglynTaskAttributeMapper mapper = getAttributeMapper(repository);
        TaskData data = new TaskData(mapper, connector.getConnectorKind(), repository.getRepositoryUrl(), fbCase.getCaseID().toString());

        fillTaskDataFromFogBugzCase(fbCase, data, mapper, repository, client, true);
        
        return data;
    }

    private void fillTaskDataFromFogBugzCase(FogBugzCase fbCase, TaskData data, FoglynTaskAttributeMapper mapper, TaskRepository repository, FogBugzClient client, boolean fromRemoteRepository) {
        mapper.setBooleanValue(Utils.createAttribute(data, FoglynAttribute.OPEN), fbCase.isOpen());
        mapper.setValue(Utils.createAttribute(data, FoglynAttribute.TITLE), fbCase.getTitle());
        
        // Project
        TaskAttribute projectAttribute = Utils.createAttribute(data, FoglynAttribute.PROJECT_ID);
        projectAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDENCY_KEY, FoglynConstants.Dependency.PROJECT.getKey());
        setAttributeID(mapper, projectAttribute, fbCase.getProject());
        setProjectOptions(projectAttribute, client);

        // Area
        TaskAttribute areaAttribute = Utils.createAttribute(data, FoglynAttribute.AREA_ID);
        // areaAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDENCY_KEY, FoglynConstants.Dependency.AREA.getKey());
        areaAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDS_ON, FoglynConstants.createDependsOn(EnumSet.of(Dependency.PROJECT)));
        setAttributeID(mapper, areaAttribute, fbCase.getArea());
        setAreaOptions(areaAttribute, client);
        
        // Assigned to
        TaskAttribute assignedToAttribute = Utils.createAttribute(data, FoglynAttribute.ASSIGNED_TO_PERSON_ID);
        // assignedToAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDS_ON, FoglynConstants.createDependsOn(EnumSet.of(Dependency.AREA)));
        // Don't set default assigned to person when case is closed
        assignedToAttribute.getMetaData().putValue(FoglynConstants.META_SET_DEFAULT, Boolean.toString(fbCase.isOpen()));
        
        setAttributeID(mapper, assignedToAttribute, fbCase.getAssignedTo());
        setPeopleOptions(assignedToAttribute, client);
        
        // Assigned to current user
        boolean assignedToCurrentUser = Utils.equal(client.getCurrentUser(), fbCase.getAssignedTo());
        mapper.setBooleanValue(Utils.createAttribute(data, FoglynAttribute.ASSIGNED_TO_CURRENT_USER), assignedToCurrentUser);

        if (fromRemoteRepository) {
            mapper.setValue(Utils.createAttribute(data, FoglynAttribute.OPENED_BY_PERSON_ID), fbCase.getOpenedBy().toString());
        }
        
        // Status
        TaskAttribute statusAttribute = Utils.createAttribute(data, FoglynAttribute.STATUS_ID);
        setAttributeID(mapper, statusAttribute, fbCase.getStatus());
        setSimpleStatusOptions(statusAttribute, client);
        
        boolean resolvedStatus = false;
        Collection<FogBugzStatus> statuses = client.getAllStatuses();
        for (FogBugzStatus fbs: statuses) {
            if (fbs.getID().equals(fbCase.getStatus())) {
                resolvedStatus = fbs.isResolved();
                break;
            }
        }
        
        mapper.setBooleanValue(Utils.createAttribute(data, FoglynAttribute.STATUS_IS_RESOLVED), resolvedStatus);

        // Status (editable status attribute, displayed in Actions part)
        TaskAttribute editableResolvedStatusAttribute = Utils.createAttribute(data, FoglynAttribute.EDITABLE_RESOLVED_STATUS_ID);
        editableResolvedStatusAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDS_ON, FoglynConstants.createDependsOn(EnumSet.of(Dependency.CATEGORY)));
        setEditableStatusOptions(editableResolvedStatusAttribute, client, true);
        setAttributeID(mapper, editableResolvedStatusAttribute, getDefaultEditableResolvedStatus(fbCase.getStatus(), fbCase.getCategory(), client));

        if (client.isFogBugz7Repository()) {
            // Add support for editing status
            TaskAttribute editableActiveStatusAttribute = Utils.createAttribute(data, FoglynAttribute.EDITABLE_ACTIVE_STATUS_ID);
            editableActiveStatusAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDS_ON, FoglynConstants.createDependsOn(EnumSet.of(Dependency.CATEGORY)));
            setEditableStatusOptions(editableActiveStatusAttribute, client, false);
            setAttributeID(mapper, editableActiveStatusAttribute, getDefaultEditableActiveStatus(fbCase.getStatus(), fbCase.getCategory(), client));
        }
        
        // Category
        TaskAttribute categoryAttribute = Utils.createAttribute(data, FoglynAttribute.CATEGORY_ID);
        categoryAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDENCY_KEY, FoglynConstants.Dependency.CATEGORY.getKey());
        setAttributeID(mapper, categoryAttribute, fbCase.getCategory());
        setCategoryOptions(categoryAttribute, client);
        
        // Priority
        TaskAttribute priorityAttribute = Utils.createAttribute(data, FoglynAttribute.PRIORITY_ID);
        setAttributeID(mapper, priorityAttribute, fbCase.getPriority());
        setPriorityOptions(priorityAttribute, client);

        // Fix for
        TaskAttribute fixForAttribute = Utils.createAttribute(data, FoglynAttribute.FIX_FOR);
        if (client.isFogBugz7Repository()) {
            fixForAttribute.getMetaData().setLabel("Milestone:");
        }
        
        fixForAttribute.getMetaData().putValue(FoglynConstants.META_DEPENDS_ON, FoglynConstants.createDependsOn(EnumSet.of(Dependency.PROJECT)));
        setAttributeID(mapper, fixForAttribute, fbCase.getFixFor());
        setFixForOptions(fixForAttribute, fbCase.getFixFor(), client);

        // Opened date
        mapper.setDateValue(Utils.createAttribute(data, FoglynAttribute.OPENED_DATE), fbCase.getOpenedDate());

        if (client.getSiteWorkingSchedule() != null) {
            // Due date
            TaskAttribute dueAttribute = Utils.createAttribute(data, FoglynAttribute.DUE_DATE);
            mapper.setDateValue(dueAttribute, fbCase.getDueDate());
            dueAttribute.getMetaData().putValue(FoglynConstants.META_WORKDAY_START, client.getSiteWorkingSchedule().getWorkdayStart().toPlainString());
            
            boolean nonEmptyPreviousValue = false;
            if (fromRemoteRepository) {
                nonEmptyPreviousValue = fbCase.getDueDate() != null;
            }
            dueAttribute.getMetaData().putValue(FoglynConstants.META_NON_EMPTY_PREVIOUS_VALUE, Boolean.toString(nonEmptyPreviousValue));
            
            // Original estimate
            if (fbCase.getOriginalEstimateInHours() != null) {
                Utils.createAttribute(data, FoglynAttribute.ORIGINAL_ESTIMATE_HOURS).setValue(fbCase.getOriginalEstimateInHours().toPlainString());
                Utils.createAttribute(data, FoglynAttribute.ORIGINAL_ESTIMATE_DHM).setValue(fbCase.getConvertedOriginalEstimate().toString());
            }
    
            // Current estimate
            TaskAttribute dhm = null;
            if (fbCase.getCurrentEstimateInHours() != null) {
                Utils.createAttribute(data, FoglynAttribute.CURRENT_ESTIMATE_HOURS).setValue(fbCase.getCurrentEstimateInHours().toPlainString());
                dhm = Utils.createAttribute(data, FoglynAttribute.CURRENT_ESTIMATE_DHM);
                dhm.setValue(fbCase.getConvertedCurrentEstimate().toString());
            } else {
                // create empty attribute, so user can update it.
                dhm = Utils.createAttribute(data, FoglynAttribute.CURRENT_ESTIMATE_DHM);
            }
            dhm.getMetaData().putValue(FoglynConstants.META_WORKING_HOURS_PER_DAY, client.getSiteWorkingSchedule().getWorkingHoursPerDay().toPlainString());
            dhm.getMetaData().putValue(FoglynConstants.META_NON_EMPTY_PREVIOUS_VALUE, Boolean.toString(fbCase.getCurrentEstimateInHours() != null));
    
            if (fbCase.getRemainingTimeInHours() != null) {
                Utils.createAttribute(data, FoglynAttribute.REMAINING_TIME_HOURS).setValue(fbCase.getRemainingTimeInHours().toPlainString());
                Utils.createAttribute(data, FoglynAttribute.REMAINING_TIME_DHM).setValue(fbCase.getConvertedRemainingTime().toString());
            }
    
            if (fbCase.getElapsedTimeInHours() != null) {
                Utils.createAttribute(data, FoglynAttribute.ELAPSED_TIME_HOURS).setValue(fbCase.getElapsedTimeInHours().toPlainString());
                Utils.createAttribute(data, FoglynAttribute.ELAPSED_TIME_DHM).setValue(fbCase.getConvertedElapsedTime().toString());
            }
        }

        // Following attributes are created only if this case is from remote repository (i.e. not new case)
        if (fromRemoteRepository) {
            mapper.setValue(Utils.createAttribute(data, FoglynAttribute.MYLYN_URL), fbCase.getTaskURL());

            mapper.setValue(Utils.createAttribute(data, FoglynAttribute.BUG_ID), fbCase.getCaseID().toString());
            mapper.setValue(Utils.createAttribute(data, FoglynAttribute.LATEST_EVENT_ID), fbCase.getLatestEvent().toString());
            mapper.setDateValue(Utils.createAttribute(data, FoglynAttribute.LAST_UPDATED_DATE), fbCase.getLastUpdated());

            // Resolved, closed date
            mapper.setDateValue(Utils.createAttribute(data, FoglynAttribute.RESOLVED_DATE), fbCase.getResolvedDate());
            mapper.setDateValue(Utils.createAttribute(data, FoglynAttribute.CLOSED_DATE), fbCase.getClosedDate());
        }
        
        // Related cases
        List<CaseID> related = fbCase.getRelatedBugs();
        if (!related.isEmpty()) {
            Utils.createAttribute(data, FoglynAttribute.RELATED_CASES).setValues(convertToStrings(related));
        }

        Utils.createAttribute(data, FoglynAttribute.REPOSITORY_IS_FOGBUGZ7_REPOSITORY).setValue(Boolean.toString(client.isFogBugz7Repository()));
        
        if (client.isFogBugz7Repository()) {
            TaskAttribute parentCaseAttr = Utils.createAttribute(data, FoglynAttribute.PARENT_CASE);
            CaseID parentCase = fbCase.getParentCase();
            if (parentCase != null) {
                parentCaseAttr.setValue(parentCase.toString());
            }
            
            List<CaseID> childrenCases = fbCase.getChildrenCases();
            // since subcases are not editable, we display it only when there is some subcase
            if (!childrenCases.isEmpty()) {
                Utils.createAttribute(data, FoglynAttribute.CHILDREN_CASES).setValues(convertToStrings(childrenCases));
            }
            
            TaskAttribute tagsAttr = Utils.createAttribute(data, FoglynAttribute.TAGS);
            tagsAttr.setValue(Utils.convertToTagsValue(fbCase.getTags()));
        }
        
        // set events
        List<FogBugzEvent> events = fbCase.getCaseEvents();
        if (events != null) {
            createEventsAndAttachmentsAttributes(data, events, repository, client);
        }
        
        // Mylyn internal attributes (displayed in the UI)
        FogBugzPriority priority = client.getPriority(fbCase.getPriority());
        if (priority != null) {
            mapper.setValue(Utils.createAttribute(data, FoglynAttribute.MYLYN_PRIORITY), priority.toUserString());
        }

        FogBugzStatus status = client.getStatus(fbCase.getStatus());
        if (status != null) {
            mapper.setValue(Utils.createAttribute(data, FoglynAttribute.MYLYN_STATUS), status.getName());
        }

        if (fbCase.getOpenedBy() != null) {
            FogBugzPerson person = client.getPerson(fbCase.getOpenedBy());
            if (person != null) {
                mapper.setValue(Utils.createAttribute(data, FoglynAttribute.MYLYN_REPORTER), person.getFullName());
            }
        }

        mapper.setValue(Utils.createAttribute(data, FoglynAttribute.MYLYN_NEW_COMMENT), "");

        // Actions
        if (fromRemoteRepository) {
            boolean isResolved = false;
            
            StatusID curstat = fbCase.getStatus();
            if (curstat != null) {
                FogBugzStatus s = client.getStatus(curstat);
                if (s != null) {
                    isResolved = s.isResolved();
                }
            }
            
            createActionAttributes(data, fbCase.getActions(), fbCase.isOpen(), isResolved, client.isFogBugz7Repository());
        }

        CaseAction action = CaseAction.EDIT;
        if (!fromRemoteRepository) {
            action = CaseAction.NEW;
        }
        mapper.setValue(Utils.createAttribute(data, FoglynAttribute.MYLYN_OPERATION), action.name());
    }

    private List<String> convertToStrings(List<CaseID> caseList) {
        List<String> cases = new ArrayList<String>();
        for (CaseID cid: caseList) {
            cases.add(cid.toString());
        }
        
        return cases;
    }

    private void createEventsAndAttachmentsAttributes(TaskData data, List<FogBugzEvent> events, TaskRepository repository, FogBugzClient client) {
        int attachmentID = 1;
        int count = 1;

        for (FogBugzEvent e: events) {
            FoglynTaskCommentMapper m = new FoglynTaskCommentMapper();
            m.setCommentId(e.getEventID().toString());
            
            FogBugzPerson eventPerson = client.getPerson(e.getInitiator());

            IRepositoryPerson initiator = null;
            if (eventPerson != null) {
                initiator = repository.createPerson(eventPerson.getEmail());
                initiator.setName(eventPerson.getFullName());
            }
                
            if (initiator != null) {
                m.setAuthor(initiator);
            }
            
            IRepositoryPerson attachmentInitiator = initiator;
            if (e.isEmail() && e.getEmailFrom() != null) {
                attachmentInitiator = repository.createPerson(e.getEmailFrom());
            }
            
            m.setCreationDate(e.getDate());
            m.setText(e.getText());
            m.setNumber(count);
            m.setVerb(e.getVerb());
            m.setEventDescription(e.getEventDescription());
            
            StringBuilder changes = new StringBuilder();
            changes.append(e.getChanges());
            
            // Remove end of lines
            List<FogBugzAttachment> attachments = e.getAttachments();
            for (FogBugzAttachment a: attachments) {
                changes.append("Attached file " + a.getFilename() + "\n");
            }
            
            m.setChanges(changes.toString());

            TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_COMMENT + count);
            m.applyTo(attribute);
            count++;
            
            // attachments are part of events
            for (FogBugzAttachment a: attachments) {
                TaskAttachmentMapper am = new TaskAttachmentMapper();
                
                if (attachmentInitiator != null) {
                    am.setAuthor(attachmentInitiator);
                }
                
                am.setFileName(a.getFilename());
                am.setLength(a.getLength());
                am.setCreationDate(e.getDate());
                am.setContentType(a.getMimetype());

                // Don't set URL of attachment ... double-clicking on attachment will not open browser. Mylyn 3.4 introduced better attachment handling, so use that.
                am.setUrl(null);

                am.setAttachmentId(String.valueOf(attachmentID));

                String comment = e.getText();
                am.setComment(comment);
                
                int eol = comment.indexOf('\n');
                if (eol >= 0) {
                    String firstLine = comment.substring(0, eol);
                    boolean isScreenshot = "Screenshot".equals(firstLine);
                    boolean isMylynContext = "mylyn/context/zip".equals(firstLine);

                    boolean firstLineUsed = false;
                    if (isScreenshot || isMylynContext) {
                        am.setDescription(firstLine);
                        firstLineUsed = true;
                    } else if (firstLine.startsWith(FoglynConstants.ATTACHMENT_DESCRIPTION_PREFIX)) {
                        am.setDescription(firstLine.substring(FoglynConstants.ATTACHMENT_DESCRIPTION_PREFIX.length()));
                        firstLineUsed = true;
                    } else if (firstLine.startsWith(FoglynConstants.PATCH_DESCRIPTION_PREFIX)) {
                        am.setDescription(firstLine.substring(FoglynConstants.PATCH_DESCRIPTION_PREFIX.length()));
                        am.setPatch(Boolean.TRUE);
                        firstLineUsed = true;
                    }
                    
                    if (firstLineUsed) {
                        if (comment.length() > eol + 1) {
                            am.setComment(comment.substring(eol + 1));
                        } else {
                            am.setComment("");
                        }
                    }
                }
                
                TaskAttribute attachAttribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_ATTACHMENT + Integer.toString(attachmentID));
                am.applyTo(attachAttribute);
                
                Utils.createAttribute(attachAttribute, FoglynAttribute.ATTACHMENT_URL_COMPONENT).setValue(a.getUrlComponent());

                attachmentID ++;
            }
        }
    }

    private void createActionAttributes(TaskData data, Set<CaseAction> actions, boolean isCaseOpen, boolean isResolved, boolean isFogBugz7Repository) {
        for (CaseAction action: actions) {
            boolean canReassign = false;
            String label = "";
            switch (action) {
            case ASSIGN: continue;
            case EMAIL:
            case FORWARD:
            case REPLY:
                // not supported
                continue;
            case NEW: continue;
            case CLOSE: label = "Close"; break;
            case EDIT:
                label = "Edit";
                // Cannot reassign when case is closed
                if (isCaseOpen) {
                    canReassign = true;
                }
                break;
            case REACTIVATE: label = "Reactivate"; canReassign = true; break;
            case REOPEN: label = "Reopen"; canReassign = true; break;
            case RESOLVE: label = "Resolve"; break;
            }
            
            TaskAttribute a = data.getRoot().createAttribute(TaskAttribute.PREFIX_OPERATION + action.name());
            // following three lines are useless, as TaskOperation.applyTo removes Kind, and sets type and readOnly metadata
            a.getMetaData().setType(TaskAttribute.TYPE_OPERATION);
            a.getMetaData().setKind(TaskAttribute.KIND_OPERATION);
            a.getMetaData().setReadOnly(true);
            if (canReassign) {
                a.getMetaData().putValue(FoglynConstants.META_OPERATION_CAN_REASSIGN, Boolean.toString(canReassign));
            }
            
            if (isFogBugz7Repository) {
                boolean editActiveCase = isCaseOpen && !isResolved && action == CaseAction.EDIT;
                boolean reactivateResolvedCase = isCaseOpen && isResolved && action == CaseAction.REACTIVATE;
                boolean reopenClosedCase = !isCaseOpen && action == CaseAction.REOPEN;
                
                if (editActiveCase || reactivateResolvedCase || reopenClosedCase) {
                    a.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, FoglynAttribute.EDITABLE_ACTIVE_STATUS_ID.getKey());
                }
            }
            
            if (action == CaseAction.RESOLVE) {
                a.getMetaData().putValue(TaskAttribute.META_ASSOCIATED_ATTRIBUTE_ID, FoglynAttribute.EDITABLE_RESOLVED_STATUS_ID.getKey());
            }
            
            TaskOperation.applyTo(a, action.name(), label);
        }
    }

    private void setFixForOptions(TaskAttribute fixForAttribute, FixForID fixFor, FogBugzClient client) {
        Collection<FogBugzFixFor> fixFors = client.getAllFixFors();
        for (FogBugzFixFor ff: fixFors) {
            boolean include = !ff.isDeleted() || fixFor.equals(ff.getID());

            if (include) {
                String name = ff.getName();
                Date d = ff.getDate();
                if (d != null) {
                    String formattedDate = DateFormat.getDateInstance(DateFormat.SHORT).format(d);
                    name = name + ": " + formattedDate;
                }

                TaskAttribute fixForAttr = fixForAttribute.createAttribute(ff.getID().toString());
                fixForAttr.setValue(name);
                
                TaskAttributeMetaData metaData = fixForAttr.getMetaData();
                metaData.setType(FoglynConstants.TYPE_OPTION_VALUE);
                if (ff.getProject() != null) {
                    metaData.putValue(Dependency.PROJECT.getKey(), ff.getProject().toString());
                }
                
                fixForAttribute.putOption(ff.getID().toString(), name);
            }
        }
    }

    /**
     * @return default status which can be edited by user. If case is in Active state,
     *      default status is based on category, and its default status. If case is not
     *      in active status, this status is used.
     */
    private StatusID getDefaultEditableResolvedStatus(StatusID currentStatus, CategoryID caseCategory, FogBugzClient client) {
        if (currentStatus != null) {
            FogBugzStatus fbs = client.getStatus(currentStatus);
            if (fbs != null && fbs.isResolved()) {
                return currentStatus;
            }
        }
        
        if (caseCategory != null) {
            FogBugzCategory fc = client.getCategory(caseCategory);
            if (fc != null) {
                return fc.getDefaultResolvedStatus();
            }
        }
        
        return null;
    }

    /**
     * @return default status which can be edited by user. If case is in Active state,
     *      default status is based on category, and its default status. If case is not
     *      in active status, this status is used.
     */
    private StatusID getDefaultEditableActiveStatus(StatusID currentStatus, CategoryID caseCategory, FogBugzClient client) {
        if (currentStatus != null) {
            FogBugzStatus fbs = client.getStatus(currentStatus);
            if (fbs != null && !fbs.isResolved()) {
                return currentStatus;
            }
        }
        
        if (caseCategory != null) {
            FogBugzCategory fc = client.getCategory(caseCategory);
            if (fc != null) {
                return fc.getDefaultActiveStatus();
            }
        }
        
        return null;
    }
    
    private void setAreaOptions(TaskAttribute areaAttribute, FogBugzClient client) {
        Collection<FogBugzArea> areas = client.getAllAreas();
        for (FogBugzArea a: areas) {
            TaskAttribute areaValAttr = areaAttribute.createAttribute(a.getID().toString());
            areaValAttr.setValue(a.getName());
            
            TaskAttributeMetaData metaData = areaValAttr.getMetaData();
            metaData.putValue(Dependency.PROJECT.getKey(), a.getProject().toString());

            areaAttribute.putOption(a.getID().toString(), a.getName());
        }
    }
    
    private void setProjectOptions(TaskAttribute projectAttribute, FogBugzClient client) {
        Collection<FogBugzProject> projects = client.getAllProjects();
        for (FogBugzProject p: projects) {
            TaskAttribute attr = projectAttribute.createAttribute(p.getID().toString());
            
            attr.setValue(p.getName());
            
            TaskAttributeMetaData metaData = attr.getMetaData();
            metaData.setType(FoglynConstants.TYPE_OPTION_VALUE);
            
            // to keep Mylyn happy (for displaying small status windows about what has changed)
            projectAttribute.putOption(p.getID().toString(), p.getName());
        }
    }

    private void setPeopleOptions(TaskAttribute personAttribute, FogBugzClient client) {
        // add primary contacts for areas
//        Collection<FogBugzArea> areas = client.getAllAreas();
//        for (FogBugzArea a: areas) {
//            PersonID ownerID = a.getOwner();
//            if (ownerID == null) {
//                FogBugzProject project = client.getProject(a.getProject());
//                if (project != null) {
//                    ownerID = project.getOwner();
//                }
//            }
//            
//            FogBugzPerson owner = null;
//            if (ownerID != null) {
//                owner = client.getPerson(ownerID);
//            }
//            
//            if (owner != null) {
//                TaskAttribute attr = personAttribute.createAttribute("primary-" + a.getID().toString());
//                attr.setValue("Primary Contact (" + owner.getFullName() + ")");
//                
//                TaskAttributeMetaData metaData = attr.getMetaData();
//                metaData.setType(FoglynConstants.TYPE_OPTION_VALUE);
//                metaData.putValue(FoglynConstants.Dependency.AREA.getKey(), a.getID().toString());
//                metaData.putValue(FoglynConstants.META_VALUE_ID, owner.getID().toString());
//                metaData.putValue(FoglynConstants.META_DEFAULT_VALUE, Boolean.toString(true));
//            }
//        }

        Collection<FogBugzPerson> people = client.getAllPeople();
        for (FogBugzPerson p: people) {
            if (!p.isInactive()) {
                TaskAttribute attr = personAttribute.createAttribute(p.getID().toString());
                attr.setValue(p.getFormattedPerson());
                
                TaskAttributeMetaData metaData = attr.getMetaData();
                metaData.setType(FoglynConstants.TYPE_OPTION_VALUE);

                personAttribute.putOption(p.getID().toString(), p.getFormattedPerson());
            }
        }
    }
    
    private void setPriorityOptions(TaskAttribute priorityAttribute, FogBugzClient client) {
        Collection<FogBugzPriority> priorities = client.getAllPriorities();
        for (FogBugzPriority p: priorities) {
            priorityAttribute.putOption(p.getID().toString(), p.toUserString());
        }
    }
    
    private void setCategoryOptions(TaskAttribute categoryAttribute, FogBugzClient client) {
        Collection<FogBugzCategory> categories = client.getAllCategories();
        for (FogBugzCategory s: categories) {
            categoryAttribute.putOption(s.getID().toString(), s.getName());
        }
    }

    /*
     * creates status options for simple status attribute (we keep status in two
     * attributes -- one for displaying status, other one for editing)
     */
    private void setSimpleStatusOptions(TaskAttribute statusAttribute, FogBugzClient client) {
        Collection<FogBugzStatus> statuses = client.getAllStatuses();
        for (FogBugzStatus s: statuses) {
            statusAttribute.putOption(s.getID().toString(), s.getName());
        }
    }
    
    private void setEditableStatusOptions(TaskAttribute statusAttribute, FogBugzClient client, boolean resolved) {
        List<FogBugzStatus> statuses = new ArrayList<FogBugzStatus>(client.getAllStatuses());
        Collections.sort(statuses, new FogBugzStatus.FogBugzStatusOrderComparator());
        
        for (FogBugzStatus s: statuses) {
            if (resolved && !s.isResolved()) {
                continue;
            }
            
            if (!resolved && s.isResolved()) {
                continue;
            }
            
            TaskAttribute sa = statusAttribute.createAttribute(s.getID().toString());
            sa.setValue(s.getName());
            
            CategoryID cid = s.getCategoryID();
            if (cid != null) {
                sa.getMetaData().putValue(FoglynConstants.Dependency.CATEGORY.getKey(), cid.toString());
                
                FogBugzCategory cat = client.getCategory(cid);
                if (cat != null && (s.getID().equals(cat.getDefaultResolvedStatus()) || s.getID().equals(cat.getDefaultActiveStatus()))) {
                    sa.getMetaData().putValue(FoglynConstants.META_DEFAULT_VALUE, Boolean.TRUE.toString());
                }
            }
            
            statusAttribute.putOption(s.getID().toString(), s.getName());
        }
    }
}
