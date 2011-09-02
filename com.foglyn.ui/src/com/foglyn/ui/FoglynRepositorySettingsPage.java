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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.foglyn.core.CompletedCaseMode;
import com.foglyn.core.FoglynCoreException;
import com.foglyn.core.FoglynCorePlugin;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.helpers.CollectionContentProvider;
import com.foglyn.helpers.MapLabelProvider;

public class FoglynRepositorySettingsPage extends AbstractRepositorySettingsPage {
    private ComboViewer completedModeViewer;
    private Button workingOn;

    public FoglynRepositorySettingsPage(String title, String description, TaskRepository taskRepository) {
        super(title, description, taskRepository);

        setNeedsAdvanced(true);
        setNeedsEncoding(false);
        setNeedsValidation(true);
        
        setImageDescriptor(FoglynImages.FOGBUGZ_REPOSITORY);
    }

    @Override
    protected void createAdditionalControls(Composite parent) {
        // Working on synchronization options
        Label workingOnLabel = new Label(parent, SWT.NONE);
        workingOnLabel.setText("'Working On' synchronization:");
        
        workingOn = new Button(parent, SWT.CHECK);
        workingOn.setText("Synchronize active tasks with FogBugz");
        
        Boolean workingOnEnabled = com.foglyn.core.Utils.isWorkingOnSynchronizationEnabled(repository);
        if (workingOnEnabled == null) {
            workingOnEnabled = FoglynUIPlugin.getDefault().isWorkingOnSynchronizationEnabled();
        }
        
        workingOn.setSelection(workingOnEnabled.booleanValue());

        // Completed case mode
        Label additional = new Label(parent, SWT.NONE);
        additional.setText("Case is marked as Complete when:");
        
        completedModeViewer = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        completedModeViewer.setContentProvider(new CollectionContentProvider(false));
        
        Map<CompletedCaseMode, String> labels = new EnumMap<CompletedCaseMode, String>(CompletedCaseMode.class);
        labels.put(CompletedCaseMode.CLOSED, "Closed");
        labels.put(CompletedCaseMode.RESOLVED_OR_CLOSED, "Closed or Resolved");
        labels.put(CompletedCaseMode.SMART_RESOLVED_OR_CLOSED, "Closed if mine, Resolved or Closed otherwise");
        
        completedModeViewer.setLabelProvider(new MapLabelProvider(labels, Collections.<Object, Image>emptyMap()));
        completedModeViewer.setInput(Arrays.asList(CompletedCaseMode.values()));
        
        // repository can be null -- that's OK
        completedModeViewer.setSelection(new StructuredSelection(com.foglyn.core.Utils.getCompletedCaseMode(repository)));
        completedModeViewer.getCombo().setVisibleItemCount(Constants.NUMBER_OF_ENTRIES_IN_COMBOBOX);
        
        GridDataFactory.defaultsFor(workingOnLabel).align(SWT.END, SWT.CENTER).applyTo(workingOnLabel);
        GridDataFactory.defaultsFor(workingOn).applyTo(workingOn);

        GridDataFactory.defaultsFor(additional).align(SWT.END, SWT.CENTER).applyTo(additional);
        GridDataFactory.defaultsFor(completedModeViewer.getControl()).applyTo(completedModeViewer.getControl());
    }

    @Override
    public String getConnectorKind() {
        return FoglynCorePlugin.CONNECTOR_KIND;
    }

    @Override
    protected Validator getValidator(final TaskRepository repository) {
        return new Validator() {
            @Override
            public void run(IProgressMonitor monitor) throws CoreException {
                AbstractWebLocation location = new TaskRepositoryLocationFactory().createWebLocation(repository);

                try {
                    FoglynCorePlugin.getDefault().getClientFactory().validateCredentials(monitor, location);

                    setStatus(Status.OK_STATUS);
                } catch (FogBugzException e) {
                    StatusHandler.log(new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, e.getMessage(), e));
                    
                    throw new FoglynCoreException(e);
                }
            }
        };
    }

    @Override
    protected boolean isValidUrl(String name) {
        URI uri;
        try {
            uri = new URI(name);
        } catch (URISyntaxException e) {
            return false;
        }
        
        if (!uri.isAbsolute()) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }

        scheme = scheme.toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.trim().length() == 0) {
            return false;
        }

        return true;
    }
    
    @Override
    public void applyTo(TaskRepository repository) {
        super.applyTo(repository);
        
        CompletedCaseMode mode = null;
        
        ISelection sel = completedModeViewer.getSelection();
        if (sel instanceof StructuredSelection) {
            Object firstElement = ((StructuredSelection) sel).getFirstElement();
            if (firstElement instanceof CompletedCaseMode) {
                mode = (CompletedCaseMode) firstElement;
            }
        }
        
        com.foglyn.core.Utils.setCompletedCaseMode(repository, mode);
        com.foglyn.core.Utils.setWorkingOnSynchronization(repository, workingOn.getSelection());
    }
}
