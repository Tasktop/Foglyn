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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.foglyn.core.FoglynCorePlugin;

public class WorkingOnCaseErrorDialog extends Dialog {

    private final String taskID;
    private final String repositoryURL;
    private final String repositoryLabel;
    
    private final IStatus activationStatus;
    
    private Button workingOnSynchronization;
    
    protected WorkingOnCaseErrorDialog(Shell shell, String taskID, String repositoryURL, String reposLabel, IStatus activationStatus) {
        super(shell);
        
        Assert.isNotNull(taskID);
        Assert.isNotNull(repositoryURL);
        Assert.isNotNull(activationStatus);
        
        this.taskID = taskID;
        this.repositoryURL = repositoryURL;
        this.repositoryLabel = reposLabel;
        this.activationStatus = activationStatus;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        Label label1 = new Label(composite, SWT.WRAP);
        label1.setText("Activation of FogBugz case " + taskID + " (" + repositoryLabel + ") failed due to following reason:");
        
        Label label = new Label(composite, SWT.WRAP);
        
        if (isMissingEstimateProblem()) {
            label.setText("Case " + taskID + " doesn't have an estimate.");
        } else {
            label.setText(activationStatus.getMessage());
        }
        
        workingOnSynchronization = new Button(composite, SWT.CHECK);
        workingOnSynchronization.setText("Synchronize active tasks with FogBugz");
        workingOnSynchronization.setSelection(FoglynUIPlugin.getDefault().isWorkingOnSynchronizationEnabled());
        
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
        GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.END).indent(0, 20).applyTo(workingOnSynchronization);

        applyDialogFont(composite);
        
        return composite;
    }
    
    protected void createButtonsForButtonBar(Composite parent) {
        if (isMissingEstimateProblem()) {
            createButton(parent, IDialogConstants.OPEN_ID, "Open Case " + taskID, false);
        }
        
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    private boolean isMissingEstimateProblem() {
        return activationStatus != null && FoglynUIPlugin.PLUGIN_ID.equals(activationStatus.getPlugin()) && activationStatus.getSeverity() == IStatus.WARNING;
    }

    private TaskRepository getTaskRepository() {
        return TasksUi.getRepositoryManager().getRepository(FoglynCorePlugin.CONNECTOR_KIND, repositoryURL);
    }
    
    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
        case IDialogConstants.OPEN_ID:
            TasksUiUtil.openTask(repositoryURL, taskID, null);
            break;
        default:
            // OK
        }

        if (workingOnSynchronization != null) {
            com.foglyn.core.Utils.setWorkingOnSynchronization(getTaskRepository(), workingOnSynchronization.getSelection());
        }
        
        setReturnCode(IDialogConstants.OK_ID);
        close();
    }
    
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        newShell.setText("Activation of Case Failed");
    }
}
