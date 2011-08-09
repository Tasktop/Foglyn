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

package com.foglyn.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.foglyn.core.FilterQuery;
import com.foglyn.core.FoglynCorePlugin;
import com.foglyn.core.FoglynRepositoryConnector;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzFilter;
import com.foglyn.helpers.CollectionContentProvider;
import com.foglyn.helpers.FilterLabelProvider;

public class FoglynFilterQueryPage extends AbstractRepositoryQueryPage {
    private ListViewer filtersList = null;
    private Text queryText;
    private boolean automaticFilterNameSet;

    private boolean firstTime = true;
    
    private FilterQuery queryToEdit;

    public FoglynFilterQueryPage(TaskRepository taskRepository) {
        this(taskRepository, null, null);
        
        setImageDescriptor(FoglynImages.FOGBUGZ_REPOSITORY);
    }
    
    public FoglynFilterQueryPage(TaskRepository taskRepository, IRepositoryQuery repositoryQuery, FilterQuery queryToEdit) {
        super("Create query based on FogBugz filter", taskRepository, repositoryQuery);
        
        setDescription("Choose filter to use for query");

        this.queryToEdit = queryToEdit;
        
        if (queryToEdit == null) {
            this.automaticFilterNameSet = true;
        } else {
            // don't modify filter name
            this.automaticFilterNameSet = false;
        }
    }
    @Override
    public String getQueryTitle() {
        return queryText.getText();
    }

    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NO_SCROLL);
        
        Label queryNameLabel = new Label(control, SWT.RIGHT);
        queryNameLabel.setText("Query name:");
        
        queryText = new Text(control, SWT.SINGLE | SWT.BORDER);
        
        Label filtersLabel = new Label(control, SWT.RIGHT);
        filtersLabel.setText("Choose filter:");

        filtersList = new ListViewer(control, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        
        // set correct margin
        GC gc = new GC(control);
        gc.setFont(control.getFont());
        FontMetrics fm = gc.getFontMetrics();
        gc.dispose();
        
        int margin = Dialog.convertHorizontalDLUsToPixels(fm, IDialogConstants.HORIZONTAL_MARGIN);
        
        // layout
        // parent has 5 pixel margin already :-(
        GridLayoutFactory.fillDefaults().numColumns(2).margins(margin - 5, 0).applyTo(control); 
        
        GridDataFactory.fillDefaults().align(SWT.TRAIL, SWT.CENTER).applyTo(queryNameLabel);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(queryText);
        GridDataFactory.fillDefaults().align(SWT.TRAIL, SWT.BEGINNING).indent(0, 2).applyTo(filtersLabel);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(filtersList.getControl());
        
        setControl(control);

        queryText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                // ignore
            }

            public void keyReleased(KeyEvent e) {
                getContainer().updateButtons();
                
                automaticFilterNameSet = false;
            }
        });
        
        filtersList.setContentProvider(new CollectionContentProvider(false));
        filtersList.setLabelProvider(new FilterLabelProvider());
        
        filtersList.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                FogBugzFilter filter = getSelectedFilter();
                
                if (filter != null && (automaticFilterNameSet || queryText.getText().trim().length() == 0)) {
                    queryText.setText(filter.getDescription());
                    automaticFilterNameSet = true;
                }

                getContainer().updateButtons();
            }
        });

        if (queryToEdit != null) {
            queryText.setText(queryToEdit.getQueryTitle());
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible && firstTime) {
            firstTime = false;
            
            final IWizardContainer wizard = getContainer();
            // run asynchronously -- we allow dialog to get displayed, and initiate fetching of filters
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    if (getControl() != null && !getControl().isDisposed()) {
                        initializePage(wizard);
                    }
                }
            });
        }
    }

    private void initializePage(IWizardContainer wizard) {
        final FoglynRepositoryConnector connector = (FoglynRepositoryConnector) TasksUi.getRepositoryManager().getRepositoryConnector(FoglynCorePlugin.CONNECTOR_KIND);

        final AtomicReference<List<FogBugzFilter>> filtersRef = new AtomicReference<List<FogBugzFilter>>();
        
        try {
            IRunnableWithProgress runnable = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Fetching filters", IProgressMonitor.UNKNOWN);
                        FogBugzClient client = connector.getClientManager().getFogbugzClient(getTaskRepository(), monitor);
                        
                        List<FogBugzFilter> filters = client.listFilters(monitor);
    
                        filtersRef.set(filters);
                        
                        monitor.done();
                    } catch (FogBugzException e) {
                        throw new InvocationTargetException(e, e.getMessage());
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e, e.getMessage());
                    }
                }
            };
    
            wizard.run(true, true, runnable);
            
            filtersList.setInput(filtersRef.get());
            
            if (queryToEdit != null && filtersRef.get() != null) {
                IStructuredSelection sel = null;
                
                for (FogBugzFilter filter: filtersRef.get()) {
                    if (filter.getFilterID().equals(queryToEdit.getFilterID())) {
                        sel = new StructuredSelection(filter);
                        break;
                    }
                }
                
                if (sel != null) {
                    filtersList.setSelection(sel, true);
                }
            }
            
        } catch (InvocationTargetException e) {
            FoglynUIPlugin.log("Cannot fetch list of filters from FogBugz", e.getCause());
            setErrorMessage("Cannot fetch list of filters from FogBugz");
        } catch (InterruptedException e) {
            FoglynUIPlugin.log("Cannot fetch list of filters from FogBugz", e);
            setErrorMessage("Cannot fetch list of filters from FogBugz");
        }
    }
    
    private FogBugzFilter getSelectedFilter() {
        IStructuredSelection selection = (IStructuredSelection) filtersList.getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        
        if (selection.size() != 1) {
            return null;
        }

        return (FogBugzFilter) selection.getFirstElement();
    }
    
    @Override
    public boolean isPageComplete() {
        if (!super.isPageComplete()) {
            return false;
        }

        if (getSelectedFilter() == null) {
            setErrorMessage("Please choose filter from list of filters");
            return false;
        }
        
        setErrorMessage(null);
        return true;
    }

    @Override
    public void applyTo(IRepositoryQuery query) {
        FilterQuery fq = new FilterQuery();
        
        fq.setQueryTitle(getQueryTitle());
        fq.setFilterID(getSelectedFilter().getFilterID());
        
        fq.saveToQuery(query);
    }
}
