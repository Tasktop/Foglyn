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

package com.foglyn.ui;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskActivationListener;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.IProgressConstants;

import com.foglyn.core.FoglynCorePlugin;
import com.foglyn.core.FoglynRepositoryConnector;
import com.foglyn.fogbugz.FogBugzCase.CaseID;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzResponseTimeTrackingException;

public class FoglynTaskActivationListener implements ITaskActivationListener {
    private final AtomicReference<ActivationJob> lastJob = new AtomicReference<ActivationJob>(null);

    public void preTaskActivated(ITask task) {
    }

    public void preTaskDeactivated(ITask task) {
    }

    public void taskActivated(ITask task) {
        lastJob.set(null);

        if (!FoglynCorePlugin.CONNECTOR_KIND.equals(task.getConnectorKind())) {
            return;
        }

        Boolean workingOnEnabled = com.foglyn.core.Utils.isWorkingOnSynchronizationEnabled(getTaskRepository(task.getRepositoryUrl()));
        if (workingOnEnabled == null) {
            workingOnEnabled = FoglynUIPlugin.getDefault().isWorkingOnSynchronizationEnabled();
        }
        
        if (!workingOnEnabled.booleanValue()) {
            return;
        }
        
        ActivationJob job = new ActivationJob(task.getTaskId(), task.getRepositoryUrl(), getRepositoryLabel(task));
        job.setPriority(Job.INTERACTIVE);
        job.setUser(true);
        job.schedule();

        lastJob.set(job);
    }

    public void taskDeactivated(ITask task) {
        ActivationJob job = lastJob.getAndSet(null);
        if (job == null) {
            return;
        }

        // cancel it if it is still running
        job.cancel();

        IStatus result = job.getResult();
        if (result != null && result.isOK()) {
            DeactivationJob deactivationJob = new DeactivationJob(task.getTaskId(), task.getRepositoryUrl(), getRepositoryLabel(task));
            deactivationJob.setPriority(Job.INTERACTIVE);
            deactivationJob.schedule();
        }
    }

    static TaskRepository getTaskRepository(String repositoryURL) {
        return TasksUi.getRepositoryManager().getRepository(FoglynCorePlugin.CONNECTOR_KIND, repositoryURL);
    }
    
    private String getRepositoryLabel(ITask task) {
        String repositoryLabel = "unknown";
        TaskRepository repository = getTaskRepository(task.getRepositoryUrl());
        if (repository != null) {
            repositoryLabel = repository.getRepositoryLabel();
        }
        return repositoryLabel;
    }

    private static class ActivationJob extends Job {
        private final String repositoryURL;
        private final String taskID;
        private final String repositoryLabel;

        public ActivationJob(String taskID, String repositoryURL, String repositoryLabel) {
            super("'Working On' activation of case " + taskID + " (" + repositoryLabel + ")");

            this.taskID = taskID;
            this.repositoryURL = repositoryURL;
            this.repositoryLabel = repositoryLabel;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
            setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.TRUE);

            IStatus result = executeActivation(monitor);
            
            if (result.isOK() || result.getSeverity() == IStatus.CANCEL) {
                return result;
            }
            
            TaskRepository repo = getTaskRepository(repositoryURL);
            if (com.foglyn.core.Utils.isShowErrorWhenActivationFails(repo)) {
                setProperty(IProgressConstants.ACTION_PROPERTY, getOpenCaseAction(result));
            } else {
                setProperty(IProgressConstants.ACTION_PROPERTY, getActivationFailedDialogAction(result));
                setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
            }
            
            return result;
        }

        private Action getOpenCaseAction(final IStatus status) {
            return new Action("Open Case " + taskID) {
                public void run() {
                    TasksUiUtil.openTask(repositoryURL, taskID, null);
                }
            };
        }
        
        private Action getActivationFailedDialogAction(final IStatus status) {
            return new Action("Open Case " + taskID) {
                public void run() {
                    Display disp = Display.getCurrent();
                    if (disp == null) {
                        return;
                    }
                    
                    Shell shell = disp.getActiveShell();
                    if (shell == null) {
                        return;
                    }
                    
                    WorkingOnCaseErrorDialog dialog = new WorkingOnCaseErrorDialog(shell, taskID, repositoryURL, repositoryLabel, status);
                    dialog.open();
                }
            };
        }

        @Override
        public boolean belongsTo(Object family) {
            return false;
//            if (!(family instanceof ActivationJob) || !(family instanceof DeactivationJob)) {
//                return false;
//            }
//
//            return true;
        }
        
        private IStatus executeActivation(IProgressMonitor monitor) {
            try {
                monitor.beginTask("Activating case " + taskID, IProgressMonitor.UNKNOWN);

                TaskRepository repository = TasksUi.getRepositoryManager().getRepository(FoglynCorePlugin.CONNECTOR_KIND, repositoryURL);
                FoglynRepositoryConnector frc = (FoglynRepositoryConnector) TasksUi.getRepositoryConnector(FoglynCorePlugin.CONNECTOR_KIND);
                
                if (repository == null || frc == null) {
                    return new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, "Unable to find repository for case " + taskID + ", url: " + repositoryURL);
                }

                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                FogBugzClient client = frc.getClientManager().getFogbugzClient(repository, monitor);

                client.startWork(CaseID.valueOf(taskID), monitor);
                return new Status(IStatus.OK, FoglynUIPlugin.PLUGIN_ID, "Working on case " + taskID + " (" + repositoryLabel + ")");
            } catch (CoreException e) {
                return e.getStatus();
            } catch (FogBugzResponseTimeTrackingException e) {
                // no need to log
                return new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, e.getMessage());
            } catch (FogBugzException e) {
                StatusHandler.log(new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, e.getMessage(), e));

                return new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, "Problem while activating case " + taskID + " (" + repositoryLabel + ")", e);
            } finally {
                monitor.done();
            }
        }
    }

    private static class DeactivationJob extends Job {
        private final String repositoryURL;
        private final String taskID;
        private final String repositoryLabel;

        public DeactivationJob(String taskID, String repositoryURL, String repositoryLabel) {
            super("'Working On' deactivation of case " + taskID + " (" + repositoryLabel + ")");

            this.taskID = taskID;
            this.repositoryURL = repositoryURL;
            this.repositoryLabel = repositoryLabel;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
            setProperty(IProgressConstants.KEEPONE_PROPERTY, Boolean.TRUE);

            TaskRepository repo = getTaskRepository(repositoryURL);
            if (!com.foglyn.core.Utils.isShowErrorWhenActivationFails(repo)) {
                setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
            }
            
            return executeDeactivation(monitor);
        }

        private IStatus executeDeactivation(IProgressMonitor monitor) {
            try {
                monitor.beginTask("Deactivating case " + taskID, IProgressMonitor.UNKNOWN);

                TaskRepository repository = TasksUi.getRepositoryManager().getRepository(FoglynCorePlugin.CONNECTOR_KIND, repositoryURL);
                FoglynRepositoryConnector frc = (FoglynRepositoryConnector) TasksUi.getRepositoryConnector(FoglynCorePlugin.CONNECTOR_KIND);
                
                if (repository == null || frc == null) {
                    return new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, "Unable to find repository for case " + taskID + ", url: " + repositoryURL);
                }

                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                FogBugzClient client = frc.getClientManager().getFogbugzClient(repository, monitor);

                client.stopWork(monitor);
                return new Status(IStatus.OK, FoglynUIPlugin.PLUGIN_ID, "Stopped working on case " + taskID + " (" + repositoryLabel + ")");
            } catch (CoreException e) {
                return e.getStatus();
            } catch (FogBugzResponseTimeTrackingException e) {
                return new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, e.getMessage());
            } catch (FogBugzException e) {
                StatusHandler.log(new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, e.getMessage(), e));
                
                return new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, "Problem while deactivating case " + taskID + " (" + repositoryLabel + ")", e);
            } finally {
                monitor.done();
            }
        }
    }
}
