package com.foglyn.core;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.data.TaskRelation;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

import com.foglyn.fogbugz.FogBugzCase;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzCase.CaseID;

public class FoglynRepositoryConnector extends AbstractRepositoryConnector {
    private final RepositoryDataManager repositoryDataManager;
    
    private final FogBugzClientFactory clientManager;
    
    private final FoglynTaskDataHandler taskDataHandler;

    private final FoglynTaskAttachmentHandler attachmentHandler;
    
    public FoglynRepositoryConnector() {
        IPath stateLocation = FoglynCorePlugin.getDefault().getStateLocation();
        this.repositoryDataManager = new RepositoryDataManager(stateLocation.toFile());
        
        this.clientManager = FoglynCorePlugin.getDefault().getClientFactory();
        this.taskDataHandler = new FoglynTaskDataHandler(this, repositoryDataManager);
        this.attachmentHandler = new FoglynTaskAttachmentHandler(this);
    }
    
    @Override
    public boolean canCreateNewTask(TaskRepository repository) {
        return true;
    }

    @Override
    public boolean canCreateTaskFromKey(TaskRepository repository) {
        return true;
    }

    @Override
    public String getConnectorKind() {
        return FoglynCorePlugin.CONNECTOR_KIND;
    }

    @Override
    public String getLabel() {
        return "FogBugz (version 6, 7 or 8)";
    }

    @Override
    public String getRepositoryUrlFromTaskUrl(String taskFullUrl) {
        try {
            URI uri = URI.create(taskFullUrl);
            String path = uri.getPath();
            
            if (path != null) {
                if (path.endsWith("default.asp")) {
                    path = path.replaceAll("default\\.asp$", "");
                }
                
                if (path.endsWith("default.php")) {
                    path = path.replaceAll("default\\.php$", "");
                }

                uri = uri.resolve(path);
            } else {
                uri = uri.resolve("");
            }
            
            return uri.toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getTaskIdFromTaskUrl(String taskFullUrl) {
        if (taskFullUrl == null) {
            return null;
        }
        
        URI uri = null;
        try {
            uri = URI.create(taskFullUrl);
        } catch (IllegalArgumentException e) {
            return null;
        }
            
        String query = uri.getQuery();
        if (query == null || query.trim().length() == 0) {
            return null;
        }
        
        String taskID = query.trim();
        
        int numTaskID = -1;
        
        try {
            numTaskID = Integer.valueOf(taskID);
        } catch (NumberFormatException e) {
            return null;
        }
        
        if (numTaskID < 0) {
            return null;
        }
        
        return taskID;
    }

    // This URL will be used only when case cannot be opened via rich text editor
    @Override
    public String getTaskUrl(String repositoryUrl, String taskId) {
        try {
            URI uri = URI.create(repositoryUrl);
            String path = uri.getPath();
            
            if (path == null || path.trim().length() == 0) {
                path = "/";
            }
            
            uri = uri.resolve(path + "?" + taskId);
            
            return uri.toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    @Override
    public TaskData getTaskData(TaskRepository taskRepository, String taskId, IProgressMonitor monitor) throws CoreException {
        FogBugzClient client = getClientManager().getFogbugzClient(taskRepository, monitor);
        FogBugzCase fbCase = null;
        try {
            fbCase = client.getCase(taskId, monitor);
        } catch (FogBugzException e) {
            StatusHandler.log(Utils.toStatus(e));
            
            throw new FoglynCoreException(e);
        }
        
        if (fbCase == null) {
            throw new CoreException(new Status(IStatus.ERROR, FoglynCorePlugin.PLUGIN_ID, "Case " + taskId + " doesn't exist in the repository " + taskRepository.getRepositoryLabel()));
        }
        
        return taskDataHandler.createTaskData(taskRepository, fbCase, client);
    }

    @Override
    public boolean hasTaskChanged(TaskRepository taskRepository, ITask task, TaskData taskData) {
        TaskMapper mapper = getTaskMapping(taskData);
        Date dataModDate = mapper.getModificationDate();

        // If this is full update, report task change if last full update is older.
        // This may be true even if local task modification time equals to task data modification time
        // System.out.println("Task: " + task.getTaskId() + ",  isPartial: " + taskData.isPartial());
        
        if (!taskData.isPartial()) {
            String lastFullUpdateTime = task.getAttribute(FoglynConstants.TASK_ATTRIBUTE_FULL_TASKDATA_LAST_UPDATE);
            if (lastFullUpdateTime != null) {
                long time = Long.parseLong(lastFullUpdateTime);
                
                // System.out.println("Task: " + task.getTaskId() + ", lastFullUpdate: " + new Date(time) + ", new task data: " + dataModDate + ", should update: " + (time < dataModDate.getTime()));
                
                return time < dataModDate.getTime();
            }
        }
        
        Date taskModDate = task.getModificationDate();

        // System.out.println("task: " + task.getTaskId() + ", local task mod: " + dataModDate + ", task data mod: " + taskModDate);

        return taskModDate == null || !taskModDate.equals(dataModDate);
    }

    @Override
    public IStatus performQuery(TaskRepository repository, IRepositoryQuery query, TaskDataCollector resultCollector,
            ISynchronizationSession event, IProgressMonitor monitor) {
        FogBugzClient client;
        try {
            client = getClientManager().getFogbugzClient(repository, monitor);
        } catch (CoreException e) {
            return e.getStatus();
        }

        monitor.beginTask("Querying repository " + repository.getRepositoryUrl(), IProgressMonitor.UNKNOWN);

        List<FogBugzCase> cases = Collections.emptyList();
        
        FoglynQuery fquery = FoglynQueryFactory.queryInstance(query);
        if (fquery != null) {
            try {
                cases = fquery.search(client, monitor);
            } catch (FogBugzException e) {
                Status s = Utils.toStatus(e);
                StatusHandler.log(s);
                return s;
            }
        }
        
        for (FogBugzCase c: cases) {
            TaskData data = taskDataHandler.createTaskData(repository, c, client);
            
            // all FoglynQueries return only partial data, without events
            data.setPartial(true);
            
            resultCollector.accept(data);
        }
        
        return Status.OK_STATUS;
    }

    @Override
    public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
        FogBugzClient client = clientManager.getFogbugzClient(repository, monitor);
        try {
            client.loadCaches(monitor);
        } catch (FogBugzException e) {
            StatusHandler.log(Utils.toStatus(e));
            throw new FoglynCoreException(e);
        }
    }

    @Override
    public void updateTaskFromTaskData(TaskRepository repository, ITask task, TaskData taskData) {
        TaskMapper mapper = getTaskMapping(taskData);
        mapper.applyTo(task);
    }
    
    @Override
    public TaskMapper getTaskMapping(TaskData taskData) {
        return new FoglynTaskMapper(taskData);
    }
    
    public FogBugzClientFactory getClientManager() {
        return clientManager;
    }
    
    @Override
    public FoglynTaskDataHandler getTaskDataHandler() {
        return taskDataHandler;
    }
    
    @Override
    public AbstractTaskAttachmentHandler getTaskAttachmentHandler() {
        return attachmentHandler;
    }
    
    @Override
    public boolean hasRepositoryDueDate(TaskRepository taskRepository, ITask task, TaskData taskData) {
        FoglynTaskMapper m = new FoglynTaskMapper(taskData);
        Date due = m.getDueDate();
        
        return due != null;
    }

    @Override
    public String[] getTaskIdsFromComment(TaskRepository repository, String comment) {
        List<String> taskIDs = new ArrayList<String>();
        
        for (Pattern p: CasePatterns.getPatterns()) {
            Matcher m = p.matcher(comment);
            while (m.find()) {
                taskIDs.add(m.group(1));
            }
        }
        
        return taskIDs.toArray(new String[taskIDs.size()]);
    }
    
    @Override
    public String getTaskIdPrefix() {
        return "case";
    }
    
    @Override
    public Collection<TaskRelation> getTaskRelations(TaskData taskData) {
        List<TaskRelation> relations = new ArrayList<TaskRelation>();
        TaskAttribute attribute = taskData.getRoot().getAttribute(FoglynAttribute.CHILDREN_CASES.getKey());
        if (attribute != null) {
            List<CaseID> cases = Utils.getCaseIDsFromString(attribute.getValues());
            for (CaseID cid: cases) {
                relations.add(TaskRelation.subtask(cid.toString()));
            }
        }
        
        attribute = taskData.getRoot().getAttribute(FoglynAttribute.PARENT_CASE.getKey());
        if (attribute != null) {
            relations.add(TaskRelation.parentTask(attribute.getValue()));
        }
        
        return relations;
    }
}
