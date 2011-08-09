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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import net.miginfocom.swt.MigLayout;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.foglyn.core.AdvancedSearchQuery;
import com.foglyn.core.FilterQuery;
import com.foglyn.core.FoglynCorePlugin;
import com.foglyn.core.FoglynQuery;
import com.foglyn.core.FoglynRepositoryConnector;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzFilter;
import com.foglyn.fogbugz.FogBugzFilter.FilterID;
import com.foglyn.fogbugz.FogBugzFixFor;
import com.foglyn.fogbugz.FogBugzProject;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.helpers.CollectionContentProvider;
import com.foglyn.helpers.FilterLabelProvider;
import com.foglyn.helpers.MappingLabelProvider;
import com.foglyn.helpers.ProjectLabelProvider;

public class FoglynNewQueryWizardPage extends AbstractRepositoryQueryPage {
    private static final String SIMPLE_QUERY_WIZARD = "SIMPLE_QUERY_WIZARD";
    private static final String SIMPLE_QUERY_WIZARD_TYPE = "SIMPLE_QUERY_WIZARD_TYPE";
    private static final String QUERY_TYPE_KEY = "QueryType";
    private static final String ENABLED_FOR_QUERY_TYPE = "EnabledForQueryType";
    
    // Do not rename ...
    private enum QueryType {
        MY_CASES,
        MY_PROJECT_CASES,
        MY_MILESTONE_CASES,
        FILTER_CASES,
        ADVANCED_SEARCH
    }
    
    private Text queryName;
    private ComboViewer project;
    private ComboViewer milestone;
    private ComboViewer filter;
    
    private List<Button> radioButtons = null;
    private List<ComboViewer> viewersToDisable = null;

    private final FoglynAdvancedSearchPage searchPage;

    private boolean firstTime = true;
    
    private boolean canChangeQueryName;
    
    private FoglynQuery query;
    
    private FogBugzClient client;
    
    public FoglynNewQueryWizardPage(TaskRepository taskRepository, IRepositoryQuery queryToEdit, FoglynQuery query) {
        super("New Foglyn Query", taskRepository, queryToEdit);
        
        setDescription(null);
        setImageDescriptor(FoglynImages.FOGBUGZ_REPOSITORY);
        
        this.query = query;
        
        searchPage = new FoglynAdvancedSearchPage(taskRepository);
        
        if (queryToEdit == null) {
            this.canChangeQueryName = true;
        } else {
            this.canChangeQueryName = false;
        }
    }

    public FoglynNewQueryWizardPage(TaskRepository repository) {
        this(repository, null, null);
    }

    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NO_SCROLL);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(control);

        control.setLayout(new MigLayout("fillx", "[left]rel[fill]"));

        Label queryNameLabel = new Label(control, SWT.NONE);
        queryNameLabel.setText("Query Name:");
        
        queryName = new Text(control, SWT.SINGLE | SWT.BORDER | SWT.FILL);
        queryName.setLayoutData("grow, wrap para");
        
        Label separator = new Label(control, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData("width 100%, span, wrap para");

/// My Cases

        Button myCasesRadio = new Button(control, SWT.RADIO);
        myCasesRadio.setText("My Cases Only");
        myCasesRadio.setData(QUERY_TYPE_KEY, QueryType.MY_CASES);
        myCasesRadio.setLayoutData("span, wrap 2*unrel");

/// Project

        Button projectRadio = new Button(control, SWT.RADIO);
        projectRadio.setData(QUERY_TYPE_KEY, QueryType.MY_PROJECT_CASES);
        projectRadio.setText("My Cases for Project:");

        project = new ComboViewer(control);
        project.setContentProvider(new CollectionContentProvider(false));
        project.setLabelProvider(new ProjectLabelProvider());
        project.getControl().setLayoutData("grow, wrap 2*unrel");
        project.setData(ENABLED_FOR_QUERY_TYPE, QueryType.MY_PROJECT_CASES);
        
/// Milestone

        Button milestoneRadio = new Button(control, SWT.RADIO);
        milestoneRadio.setData(QUERY_TYPE_KEY, QueryType.MY_MILESTONE_CASES);
        milestoneRadio.setText("My Cases for Milestone:");

        milestone = new ComboViewer(control);
        milestone.setContentProvider(new CollectionContentProvider(false));
        // Label provider is set in setupMilestones method
        milestone.getControl().setLayoutData("grow, wrap 2*unrel");
        milestone.setData(ENABLED_FOR_QUERY_TYPE, QueryType.MY_MILESTONE_CASES);

/// Filter

        Button filterRadio = new Button(control, SWT.RADIO);
        filterRadio.setData(QUERY_TYPE_KEY, QueryType.FILTER_CASES);
        filterRadio.setText("Cases From FogBugz Filter:");

        filter = new ComboViewer(control);
        filter.setContentProvider(new CollectionContentProvider(false));
        filter.setLabelProvider(new FilterLabelProvider());
        filter.getControl().setLayoutData("grow, wrap 2*unrel");
        filter.setData(ENABLED_FOR_QUERY_TYPE, QueryType.FILTER_CASES);
        
/// Search options ...

        Button searchRadio = null;
        if (query == null) {
            searchRadio = new Button(control, SWT.RADIO);
            searchRadio.setText("Cases Matched by Search Options (specified on next page)");
            searchRadio.setData(QUERY_TYPE_KEY, QueryType.ADVANCED_SEARCH);
            searchRadio.setLayoutData("span, wrap 2*unrel");
        }
        
        setControl(control);
        
        // setup listeners, and stuff
        
        queryName.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                getContainer().updateButtons();
                
                canChangeQueryName = false;
            }
        });
        
        queryName.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (queryName.getText().trim().length() == 0) {
                    canChangeQueryName = true;
                    determineQueryName();
                }
            }
        });
        
        viewersToDisable = new ArrayList<ComboViewer>();
        viewersToDisable.add(project);
        viewersToDisable.add(milestone);
        viewersToDisable.add(filter);
        
        ISelectionChangedListener scl = new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                determineQueryName();
                
                getContainer().updateButtons();
            }
        };
        
        for (ComboViewer cv: viewersToDisable) {
            cv.addSelectionChangedListener(scl);
        }
        
        radioButtons = new ArrayList<Button>();
        radioButtons.add(myCasesRadio);
        radioButtons.add(projectRadio);
        radioButtons.add(milestoneRadio);
        radioButtons.add(filterRadio);
        if (searchRadio != null) {
            radioButtons.add(searchRadio);
        }

        SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!(e.widget instanceof Button)) {
                    return;
                }

                updateViewers();
                
                Button b = (Button) e.widget;
                if (b.getSelection()) {
                    determineQueryName();
                    
                    getContainer().updateButtons();
                }
            }
        };
        
        for (Button b: radioButtons) {
            b.addSelectionListener(listener);
        }
        
        updateViewers();
    }

    protected void updateViewers() {
        QueryType qt = getSelectedQueryType();
        
        for (ComboViewer v: viewersToDisable) {
            QueryType e = (QueryType) v.getData(ENABLED_FOR_QUERY_TYPE);
            
            v.getControl().setEnabled(e.equals(qt));
        }
    }

    @Override
    public String getQueryTitle() {
        return queryName.getText();
    }

    protected void determineQueryName() {
        if (!canChangeQueryName) {
            return;
        }

        QueryType qt = getSelectedQueryType();
        if (qt == null) {
            return;
        }

        String newQueryName = null;
        
        switch (qt) {
        case ADVANCED_SEARCH:
            newQueryName = "FogBugz Search ...";
            break;
        case MY_CASES:
            newQueryName = "My Cases";
            break;
        case FILTER_CASES:
            FogBugzFilter f = getSelection(filter, FogBugzFilter.class);
            if (f != null) {
                newQueryName = "Filter: " + f.getDescription();
            } else {
                newQueryName = "Filter: ...";
            }
            break;
        case MY_PROJECT_CASES:
            FogBugzProject p = getSelection(project, FogBugzProject.class);
            if (p != null) {
                newQueryName = "Project: " + p.getName();
            } else {
                newQueryName = "Project: ...";
            }
            break;
        case MY_MILESTONE_CASES:
            FogBugzFixFor ff = getSelection(milestone, FogBugzFixFor.class);
            if (ff == null) {
                newQueryName = "Milestone: ...";
            } else {
                FogBugzProject ffp = null;
                if (ff.getProject() != null && client != null) {
                    ffp = client.getProject(ff.getProject());
                }
                
                if (ffp != null) {
                    newQueryName = "Milestone: " + ffp.getName() + ": " + ff.getName();
                } else {
                    newQueryName = "Milestone: " + ff.getName();
                }
            }
            break;
        }
        
        if (newQueryName != null) {
            this.queryName.setText(newQueryName);
            canChangeQueryName = true;
        }
    }
    
    @Override
    public boolean isPageComplete() {
        if (!super.isPageComplete()) {
            return false;
        }

        QueryType qt = getSelectedQueryType();
        if (qt == null) {
            setErrorMessage("Please choose type of query");
            return false;
        }
        
        switch (qt) {
        case ADVANCED_SEARCH:
            // We are not complete for ADV. Search... user must go to Next page
            return false;
        case MY_CASES:
            return true;
        case FILTER_CASES:
            if (getSelection(filter, FogBugzFilter.class) == null) {
                setMessage("Please select filter.");
                return false;
            }
            setMessage(null);
            return true;
        case MY_MILESTONE_CASES:
            if (getSelection(milestone, FogBugzFixFor.class) == null) {
                setMessage("Please select milestone.");
                return false;
            }
            setMessage(null);
            return true;
        case MY_PROJECT_CASES:
            if (getSelection(project, FogBugzProject.class) == null) {
                setMessage("Please select project.");
                return false;
            }
            setMessage(null);
            return true;
        }
        
        setErrorMessage(null);
        return true;
    }
    
    <T> T getSelection(ComboViewer cv, Class<T> clz) {
        ISelection sel = cv.getSelection();
        if (sel == null || sel.isEmpty()) {
            return null;
        }

        IStructuredSelection iss = (IStructuredSelection) sel;
        Object o = iss.getFirstElement();
        
        return clz.cast(o);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible && firstTime) {
            firstTime = false;
            
            final IWizardContainer wizard = getContainer();
            // run asynchronously -- we allow dialog to get displayed, and initiate fetching of FogBugz data
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
        final AtomicReference<Collection<FogBugzProject>> projectsRef = new AtomicReference<Collection<FogBugzProject>>();
        final AtomicReference<Collection<FogBugzFixFor>> milestonesRef = new AtomicReference<Collection<FogBugzFixFor>>();
        final AtomicReference<FogBugzClient> fbclient = new AtomicReference<FogBugzClient>();
        
        try {
            IRunnableWithProgress runnable = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Fetching data from FogBugz server", IProgressMonitor.UNKNOWN);
                        FogBugzClient client = connector.getClientManager().getFogbugzClient(getTaskRepository(), monitor);
                        
                        filtersRef.set(client.listFilters(monitor));
                        projectsRef.set(client.getAllProjects());
                        milestonesRef.set(client.getAllFixFors());
                        fbclient.set(client);
                        
                        monitor.done();
                    } catch (FogBugzException e) {
                        throw new InvocationTargetException(e, e.getMessage());
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e, e.getMessage());
                    }
                }
            };
    
            wizard.run(true, true, runnable);

            client = fbclient.get();
            filter.setInput(filtersRef.get());
            project.setInput(projectsRef.get());

            setupMilestones(fbclient.get(), milestonesRef.get());
            
            QueryType qt = null;
            if (query != null) {
                if (query instanceof FilterQuery && filtersRef.get() != null) {
                    qt = QueryType.FILTER_CASES;
                    
                    FilterID fid = ((FilterQuery) query).getFilterID();
                    for (FogBugzFilter f: filtersRef.get()) {
                        if (f.getFilterID().equals(fid)) {
                            filter.setSelection(new StructuredSelection(f), true);
                        }
                    }
                }
                
                if (query instanceof AdvancedSearchQuery) {
                    AdvancedSearchQuery asq = (AdvancedSearchQuery) query;
                    
                    if (asq.getProject() != null && projectsRef.get() != null) {
                        qt = QueryType.MY_PROJECT_CASES;
                        
                        for (FogBugzProject p: projectsRef.get()) {
                            if (p.getID().equals(asq.getProject())) {
                                project.setSelection(new StructuredSelection(p), true);
                            }
                        }
                    } else if (asq.getFixFor() != null && milestonesRef.get() != null) {
                        qt = QueryType.MY_MILESTONE_CASES;
                        
                        for (FogBugzFixFor m: milestonesRef.get()) {
                            if (m.getID().equals(asq.getFixFor())) {
                                milestone.setSelection(new StructuredSelection(m), true);
                            }
                        }
                    }
                }
                
                if (qt != null) {
                    for (Button b: radioButtons) {
                        b.setSelection(qt.equals(b.getData(QUERY_TYPE_KEY)));
                    }
                }
                
                updateViewers();
                
                queryName.setText(query.getQueryTitle());
            }
        } catch (InvocationTargetException e) {
            FoglynUIPlugin.log("Cannot fetch data from FogBugz", e.getCause());
            setErrorMessage("Cannot fetch data from FogBugz");
        } catch (InterruptedException e) {
            FoglynUIPlugin.log("Cannot fetch data from FogBugz", e);
            setErrorMessage("Cannot fetch data from FogBugz");
        }
    }
    
    private void setupMilestones(FogBugzClient fogBugzClient, Collection<FogBugzFixFor> milestones) {
        Map<ProjectID, List<FogBugzFixFor>> projectMilestones = new HashMap<FogBugzProject.ProjectID, List<FogBugzFixFor>>();
        List<FogBugzFixFor> globalMilestones = new ArrayList<FogBugzFixFor>();

        // sort milestones into global, and project
        sortMilestones(fogBugzClient, milestones, projectMilestones, globalMilestones);
        
        Map<Object, String> milestoneLabels = new HashMap<Object, String>();
        List<Object> milestonesInput = new ArrayList<Object>();

        // global milestones first
        for (FogBugzFixFor ff: globalMilestones) {
            milestoneLabels.put(ff, getMilestoneName(ff, null, true));
            milestonesInput.add(ff);
        }
        
        for (Entry<ProjectID, List<FogBugzFixFor>> pms: projectMilestones.entrySet()) {
            FogBugzProject p = fogBugzClient.getProject(pms.getKey());

            for (FogBugzFixFor ff: pms.getValue()) {
                milestoneLabels.put(ff, getMilestoneName(ff, p, true));
                milestonesInput.add(ff);
            }
        }

        milestone.setLabelProvider(new MappingLabelProvider(milestoneLabels));
        milestone.setInput(milestonesInput);
    }

    // Sorts milestones into "global" and "per project" categories. Each category is sorted by milestone name.
    private void sortMilestones(FogBugzClient fogBugzClient, Collection<FogBugzFixFor> milestones, Map<ProjectID, List<FogBugzFixFor>> projectMilestones, List<FogBugzFixFor> globalMilestones) {
        for (FogBugzFixFor ff: milestones) {
            if (ff.isDeleted()) {
                continue;
            }
            
            ProjectID p = ff.getProject();
            if (p == null && fogBugzClient.getProject(p) != null) {
                globalMilestones.add(ff);
            } else {
                List<FogBugzFixFor> ms = projectMilestones.get(p);
                if (ms == null) {
                    ms = new ArrayList<FogBugzFixFor>();
                    projectMilestones.put(p, ms);
                }
                ms.add(ff);
            }
        }
        
        Comparator<FogBugzFixFor> comp = new Comparator<FogBugzFixFor>() {
            public int compare(FogBugzFixFor o1, FogBugzFixFor o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };
        
        // Sort milestones in each category
        Collections.sort(globalMilestones, comp);
        for (List<FogBugzFixFor> l: projectMilestones.values()) {
            Collections.sort(l, comp);
        }
    }

    private String getMilestoneName(FogBugzFixFor m, FogBugzProject project, boolean date) {
        StringBuilder sb = new StringBuilder();
        if (project != null) {
            sb.append(project.getName());
            sb.append(": ");
        }
        
        sb.append(m.getName());
        
        if (date) {
            Date d = m.getDate();
            if (d != null) {
                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                sb.append(" (");
                sb.append(df.format(d));
                sb.append(")");
            }
        }
        
        return sb.toString();
    }

    @Override
    public boolean canFlipToNextPage() {
        // We ignore query name validation. Our "isPageComplete" returns false for ADVANCED_SEARCH type, we skip it too.
        return getSelectedQueryType() == QueryType.ADVANCED_SEARCH;
    }
    
    @Override
    public IWizardPage getNextPage() {
        QueryType t = getSelectedQueryType();
        if (t == QueryType.ADVANCED_SEARCH) {
            searchPage.setWizard(this.getWizard());
            searchPage.setQueryTitle(getQueryTitle());
            return searchPage;
        }
        
        return null;
    }
    
    @Override
    public void applyTo(IRepositoryQuery query) {
        FoglynQuery q = null;
        
        QueryType qt = getSelectedQueryType();
        switch (qt) {
        case FILTER_CASES:
            q = getFilterQuery();
            break;
        case MY_CASES:
            q = getMyCasesQuery();
            break;
        case MY_MILESTONE_CASES:
            q = getMyMilestoneCasesQuery();
            break;
        case MY_PROJECT_CASES:
            q = getMyProjectCasesQuery();
            break;
        default:
            throw new IllegalStateException("Cannot happen");
        }

        q.setQueryTitle(getQueryTitle());
        query.setAttribute(SIMPLE_QUERY_WIZARD, Boolean.TRUE.toString());
        query.setAttribute(SIMPLE_QUERY_WIZARD_TYPE, qt.name());
        q.saveToQuery(query);
    }

    private FilterQuery getFilterQuery() {
        FilterQuery fq = new FilterQuery();
        
        FogBugzFilter f = getSelection(filter, FogBugzFilter.class);
        fq.setFilterID(f.getFilterID());
        return fq;
    }

    private AdvancedSearchQuery getMyCasesQuery() {
        AdvancedSearchQuery asq = new AdvancedSearchQuery();
        asq.setAssignedToMe(true);
        return asq;
    }

    private AdvancedSearchQuery getMyMilestoneCasesQuery() {
        AdvancedSearchQuery asq = new AdvancedSearchQuery();
        asq.setAssignedToMe(true);
        asq.setFixFor(getSelection(milestone, FogBugzFixFor.class).getID());
        return asq;
    }

    private AdvancedSearchQuery getMyProjectCasesQuery() {
        AdvancedSearchQuery asq = new AdvancedSearchQuery();
        asq.setAssignedToMe(true);
        asq.setProject(getSelection(project, FogBugzProject.class).getID());
        return asq;
    }
    
    private QueryType getSelectedQueryType() {
        for (Button b: radioButtons) {
            if (b.getSelection()) {
                return (QueryType) b.getData(QUERY_TYPE_KEY);
            }
        }
        
        return null;
    }
    
    static boolean isWizardQuery(IRepositoryQuery queryToEdit) {
        return Boolean.parseBoolean(queryToEdit.getAttribute(FoglynNewQueryWizardPage.SIMPLE_QUERY_WIZARD));        
    }
}
