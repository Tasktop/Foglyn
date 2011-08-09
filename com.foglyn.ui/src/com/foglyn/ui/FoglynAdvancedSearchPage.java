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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import net.miginfocom.swt.MigLayout;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskSearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.foglyn.core.AdvancedSearchQuery;
import com.foglyn.core.AdvancedSearchQuery.DateCondition;
import com.foglyn.core.FoglynCorePlugin;
import com.foglyn.core.FoglynRepositoryConnector;
import com.foglyn.fogbugz.FogBugzArea;
import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzArea.AreaIDFactory;
import com.foglyn.fogbugz.FogBugzCategory;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryIDFactory;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzFixFor;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzFixFor.FixForIDFactory;
import com.foglyn.fogbugz.FogBugzPerson;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPerson.PersonIDFactory;
import com.foglyn.fogbugz.FogBugzPriority;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityIDFactory;
import com.foglyn.fogbugz.FogBugzProject;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzProject.ProjectIDFactory;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;
import com.foglyn.fogbugz.FogBugzStatus.StatusIDFactory;
import com.foglyn.fogbugz.HasID;
import com.foglyn.fogbugz.ID;
import com.foglyn.fogbugz.IDFactory;
import com.foglyn.helpers.AreaFilter;
import com.foglyn.helpers.AreaLabelProvider;
import com.foglyn.helpers.CategoryLabelProvider;
import com.foglyn.helpers.DateConditionComboViewer;
import com.foglyn.helpers.FBDatatypeComboViewer;
import com.foglyn.helpers.FBStatusesComboViewer;
import com.foglyn.helpers.FixForFilter;
import com.foglyn.helpers.FixForLabelProvider;
import com.foglyn.helpers.HelperConstants;
import com.foglyn.helpers.NullValueFilter;
import com.foglyn.helpers.PersonLabelProvider;
import com.foglyn.helpers.PriorityLabelProvider;
import com.foglyn.helpers.ProjectLabelProvider;

public class FoglynAdvancedSearchPage extends AbstractRepositoryQueryPage {
    private static final String PAGE_NAME = "AdvancedSearchPage";

    // Used when storing dialog settings
    private static final String TAGS = "TAGS";
    private static final String SUBSCRIBED = "SUBSCRIBED";
    private static final String STARRED_BY_ME = "STARRED_BY_ME";
    private static final String CORRESPONDENT = "CORRESPONDENT";
    private static final String DUE_DATE = "DUE_DATE";
    private static final String CLOSED_DATE = "CLOSED_DATE";
    private static final String RESOLVED_DATE = "RESOLVED_DATE";
    private static final String EDITED_DATE = "EDITED_DATE";
    private static final String OPENED_DATE = "OPENED_DATE";
    private static final String PRIORITY = "PRIORITY";
    private static final String FIXFOR = "FIXFOR";
    private static final String STATUS = "STATUS";
    private static final String AREA = "AREA";
    private static final String PROJECT = "PROJECT";
    private static final String CATEGORY = "CATEGORY";
    private static final String CLOSED_BY = "CLOSED_BY";
    private static final String LAST_EDITED_BY = "LAST_EDITED_BY";
    private static final String ALSO_EDITED_BY = "ALSO_EDITED_BY";
    private static final String EDITED_BY = "EDITED_BY";
    private static final String OPENED_BY = "OPENED_BY";
    private static final String RESOLVED_BY = "RESOLVED_BY";
    private static final String ASSIGNED_TO = "ASSIGNED_TO";
    private static final String SEARCH_QUERY = "SEARCH_QUERY";

    // is this page open for the first time (i.e., do we need to initialize after opening?)
    private boolean firstTime = true;

    private Text queryTitle;
    
    private Text searchQuery;

    private FBDatatypeComboViewer<CategoryID, FogBugzCategory> category;
    private FBDatatypeComboViewer<ProjectID, FogBugzProject> project;
    private FBDatatypeComboViewer<AreaID, FogBugzArea> area;
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> assignedTo;
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> resolvedBy;
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> openedBy;
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> closedBy;
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> editedBy;
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> lastEditedBy;
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> alsoEditedBy;
    private FBStatusesComboViewer status;
    private FBDatatypeComboViewer<FixForID, FogBugzFixFor> fixFor;
    private FBDatatypeComboViewer<PriorityID, FogBugzPriority> priority;
    
    private DateConditionComboViewer due;
    private DateConditionComboViewer opened;
    private DateConditionComboViewer edited;
    private DateConditionComboViewer resolved;
    private DateConditionComboViewer closed;
    
    private Text tags;

    private Text correspondent;

    private Button starredByMe;
    private Button subscribed;

    private SearchPageChangedListener searchOptionsListener;

    private AdvancedSearchQuery initialQuery;
    private String initialQueryName;

    private Label alsoEditedByLabel;
    
    private boolean controlsEnabled = true;

    private boolean isFogBugz7Repo;
    
    public FoglynAdvancedSearchPage(TaskRepository taskRepository) {
        this(taskRepository, null, null);

        setImageDescriptor(FoglynImages.FOGBUGZ_REPOSITORY);
    }

    public FoglynAdvancedSearchPage(TaskRepository taskRepository, IRepositoryQuery reposQuery, AdvancedSearchQuery initialQuery) {
        super("FogBugz Search", taskRepository, reposQuery);

        isFogBugz7Repo = com.foglyn.core.Utils.isFogBugz7Repository(taskRepository, null, null);
        
        this.searchOptionsListener = new SearchPageChangedListener();
        this.initialQuery = initialQuery;
    }

    @Override
    public String getQueryTitle() {
        if (queryTitle == null) {
            return null;
        }
        
        // useful for query page, not for search page
        return getText(queryTitle);
    }

    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NO_SCROLL);
        // parent uses Grid Layout
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(control);

        // set layout
        control.setLayout(new MigLayout("wrap 6, gapy unrelated",
                "[right,sizegroup label]" +
                "[fill,grow,sizegroup grp]" +
                "indent" +
                "[right,sizegroup label]" +
                "[fill,grow,sizegroup grp]" +
                "indent" +
                "[right,sizegroup label]" +
                "[fill,grow,sizegroup grp]"));
        
        // addSeparator(control, "FogBugz search options");
        
        if (getSearchContainer() == null) {
            label(control, "Query name:");
            
            queryTitle = createTextField(control);
            queryTitle.setLayoutData("span");
            if (initialQueryName != null) {
                queryTitle.setText(initialQueryName);
                initialQueryName = null;
            }
            
            addSeparator(control);
        }
        
        label(control, "Search &For:");
        searchQuery = createTextField(control);
        searchQuery.setLayoutData("span");

        label(control, "&Project:");
        project = createFBDatatypeCombo(FogBugzProject.class, control, "\u2014 All Projects \u2014", new ProjectLabelProvider());
        
        label(control, "&Area:");
        area = createFBDatatypeCombo(FogBugzArea.class, control, "\u2014 All Areas \u2014", new AreaLabelProvider());

        label(control, "&Category:");
        category = createFBDatatypeCombo(FogBugzCategory.class, control, "\u2014 All \u2014", new CategoryLabelProvider());

        label(control, "S&tatus:");
        status = createFBStatusesCombo(control, "\u2014 All \u2014");

        label(control, "Priorit&y:");
        priority = createFBDatatypeCombo(FogBugzPriority.class, control, "\u2014 All \u2014", new PriorityLabelProvider());

        if (isFogBugz7Repo) {
            label(control, "Milestone:");
        } else {
            label(control, "Fi&x For:");
        }
        fixFor = createFBDatatypeCombo(FogBugzFixFor.class, control, "\u2014 All \u2014", new FixForLabelProvider());
        
        addSeparator(control);

        label(control, "&Opened By:");
        openedBy = createPeopleCombo(control);

        List<DateCondition> dateConditions = Arrays.asList(DateCondition.TODAY, DateCondition.TODAY_OR_YESTERDAY,
                DateCondition.LAST_WEEK, DateCondition.LAST_MONTH, DateCondition.LAST_2_MONTHS,
                DateCondition.LAST_3_MONTHS, DateCondition.LAST_6_MONTHS, DateCondition.LAST_YEAR);
        
        label(control, "Opened:");
        opened = createDateConditionCombo(control, dateConditions);

        label(control, "&Due:");
        due = createDateConditionCombo(control, Arrays.asList(DateCondition.IN_THE_PAST, DateCondition.TODAY, DateCondition.TODAY_OR_TOMORROW,
                DateCondition.NEXT_WEEK, DateCondition.NEXT_MONTH, DateCondition.NEXT_2_MONTHS,
                DateCondition.NEXT_3_MONTHS, DateCondition.NEXT_6_MONTHS, DateCondition.NEXT_YEAR));
        
        label(control, "&Edited By:");
        editedBy = createPeopleCombo(control);

        label(control, "Edited:");
        edited = createDateConditionCombo(control, dateConditions);

        alsoEditedByLabel = label(control, "Also Edited By:");
        alsoEditedBy = createPeopleCombo(control);
        
        label(control, "&Resolved By:");
        resolvedBy = createPeopleCombo(control);

        label(control, "Resolved:");
        resolved = createDateConditionCombo(control, dateConditions);

        label(control, "Last Edited By:");
        lastEditedBy = createPeopleCombo(control);
        
        label(control, "Closed By:");
        closedBy = createPeopleCombo(control);

        label(control, "Closed:");
        closed = createDateConditionCombo(control, dateConditions);

        label(control, "Correspondent:");
        correspondent = createTextField(control);
        
        label(control, "Assi&gned To:");
        assignedTo = createPeopleCombo(control);

        starredByMe = createCheckbox(control, "Starred by me");

        subscribed = createCheckbox(control, "Subscribed by me");

        if (isFogBugz7Repo) {
            label(control, "Tags:");
            tags = createTextField(control);
            
            // don't wrap if not in search container ... single button looks weird on its own line
            if (inSearchContainer()) {
                tags.setLayoutData("wrap");
            }
        }

        String updateButtonLayout = "width button, span, right";
        
        if (inSearchContainer()) {
            Button resetButton = new Button(control, SWT.PUSH);
            resetButton.setText("Defaults");
            resetButton.setToolTipText("Reset fields to default values");
            resetButton.setLayoutData("width button, span, split 2, right");
            resetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    resetWidgetsToDefaults();
                }
            });
            
            updateButtonLayout = "width button, right";
        }
        
        Button updateButton = new Button(control, SWT.PUSH);
        updateButton.setText("Update");
        updateButton.setToolTipText("Update attributes from repository");
        updateButton.setLayoutData(updateButtonLayout);
        updateButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateRepositoryConfiguration();
            }
        });

        // don't enable AlsoEditedBy until there is 'Edited by' person
        alsoEditedByLabel.setEnabled(false);
        alsoEditedBy.getControl().setEnabled(false);
        editedBy.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection ss = (IStructuredSelection) event.getSelection();
                
                Object selected = ss.getFirstElement();
                
                if (selected != null && !HelperConstants.NULL_VALUE.equals(selected)) {
                    enableAlsoEditedBy(true);
                } else {
                    enableAlsoEditedBy(false);
                }
            }
        });
        
        // setup filters
        final AreaFilter areaFilter = new AreaFilter();
        area.addFilter(new NullValueFilter(areaFilter));

        final FixForFilter fixForFilter = new FixForFilter();
        fixFor.addFilter(new NullValueFilter(fixForFilter));
        
        project.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ProjectID projectID = project.convertSelection(event.getSelection());
                
                areaFilter.setProjectID(projectID);
                fixForFilter.setProjectID(projectID);
                
                refreshViewer(area);
                refreshViewer(fixFor);
            }
        });
        
        category.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                CategoryID categoryID = category.convertSelection(event.getSelection());
                
                status.setsetCategoryID(categoryID);
                status.setUsePrefix(categoryID == null);
                
                refreshViewer(status);
            }
        });
        
        setControl(control);
    }

    private void enableAlsoEditedBy(boolean enabled) {
        alsoEditedByLabel.setEnabled(enabled);
        alsoEditedBy.getControl().setEnabled(enabled);
    }

    private Label label(Composite parent, String text) {
        Label l = new Label(parent, SWT.RIGHT);
        l.setText(text);
        return l;
    }
    
    // Don't forget to enable/disable buttons if we are complete.
    @Override
    public void setPageComplete(boolean complete) {
        super.setPageComplete(complete);
        
        // Update button only if we control it (i.e. our controls are enabled)
        if (getSearchContainer() != null && controlsEnabled) {
            getSearchContainer().setPerformActionEnabled(complete);
        }
    }

    // Caching of error message is done to avoid flickering when super.isPageComplete is called -- it sets error message to
    // null, although we may change it to real message later... in such case, message flickers :-(
    private boolean delaySettingErrorMessage = false;
    private String errorMessageCache;
    
    @Override
    public void setErrorMessage(String newMessage) {
        errorMessageCache = newMessage;

        if (!delaySettingErrorMessage) {
            super.setErrorMessage(newMessage);
        }
    }

    private void updateErrorMessageFromCache() {
        super.setErrorMessage(errorMessageCache);
    }
    
    @Override
    public boolean isPageComplete() {
        if (getSearchContainer() == null) {
            // this checks if query name is unique
            
            // all this trickery with caching error message is to avoid super.isPageComplete setting error message to null
            // if there is no problem, because it causes flickering :-(
            delaySettingErrorMessage = true;
            
            boolean superComplete = false;
            try {
                superComplete = super.isPageComplete();
            } finally {
                delaySettingErrorMessage = false;
            }
            
            if (!superComplete) {
                // display error message from super.isPageComplete
                updateErrorMessageFromCache();
                return false;
            }
        }

        if (!isSearchPossible()) {
            setErrorMessage("Please specify search options");
            return false;
        }
        
        setErrorMessage(null);
        return true;
    }

    private boolean isSearchPossible() {
        List<Object> values = new ArrayList<Object>();
        
        values.add(getText(searchQuery));
        
        values.add(assignedTo.getSelectedID());
        values.add(resolvedBy.getSelectedID());
        values.add(openedBy.getSelectedID());
        
        PersonID eb = editedBy.getSelectedID();
        values.add(eb);
        
        if (eb != null) {
            values.add(alsoEditedBy.getSelectedID());
        }
        
        values.add(lastEditedBy.getSelectedID());
        values.add(closedBy.getSelectedID());
        
        values.add(category.getSelectedID());
        values.add(project.getSelectedID());
        values.add(area.getSelectedID());
        values.add(status.getSelectedStatuses());
        values.add(fixFor.getSelectedID());
        values.add(priority.getSelectedID());
        
        values.add(opened.getDateCondition());
        values.add(edited.getDateCondition());
        values.add(resolved.getDateCondition());
        values.add(closed.getDateCondition());
        values.add(due.getDateCondition());
        
        values.add(getText(correspondent));
        
        if (isFogBugz7Repo && tags != null) {
            List<String> tl = com.foglyn.core.Utils.getTagsFromString(tags.getText());
            if (!tl.isEmpty()) {
                values.add(tl);
            }
        }
        
        if (starredByMe.getSelection()) {
            values.add(Boolean.TRUE);
        }
        
        if (subscribed.getSelection()) {
            values.add(Boolean.TRUE);
        }
        
        // check if any value is selected
        for (Object o: values) {
            if (o != null) return true;
        }
        
        return false;
    }

    @Override
    public void applyTo(IRepositoryQuery query) {
        AdvancedSearchQuery asq = new AdvancedSearchQuery();
        
        if (queryTitle != null) {
            asq.setQueryTitle(getText(queryTitle));
        } else {
            asq.setQueryTitle("Ad-hoc search query");
        }
        
        asq.setSearchString(getText(searchQuery));
        
        asq.setAssignedTo(assignedTo.getSelectedID());
        asq.setResolvedBy(resolvedBy.getSelectedID());
        asq.setOpenedBy(openedBy.getSelectedID());
        
        PersonID eb = editedBy.getSelectedID();
        asq.setEditedBy(eb);
        
        if (eb != null) {
            asq.setAlsoEditedBy(alsoEditedBy.getSelectedID());
        } else {
            asq.setAlsoEditedBy(null);
        }
        
        asq.setLastEditedBy(lastEditedBy.getSelectedID());
        asq.setClosedBy(closedBy.getSelectedID());
        
        asq.setCategory(category.getSelectedID());
        asq.setProject(project.getSelectedID());
        asq.setArea(area.getSelectedID());
        asq.setStatuses(status.getSelectedStatuses());
        asq.setFixFor(fixFor.getSelectedID());
        asq.setPriority(priority.getSelectedID());
        
        asq.setOpened(opened.getDateCondition());
        asq.setEdited(edited.getDateCondition());
        asq.setResolved(resolved.getDateCondition());
        asq.setClosed(closed.getDateCondition());
        asq.setDue(due.getDateCondition());
        
        asq.setCorrespondent(getText(correspondent));
        asq.setStarredByMe(starredByMe.getSelection());
        asq.setSubscribed(subscribed.getSelection());
        
        if (isFogBugz7Repo && tags != null) {
            asq.setTags(getText(tags));
        }
        
        asq.saveToQuery(query);
    }

    private String getText(Text text) {
        String t = text.getText();
        if (t != null && t.trim().length() > 0) {
            return t.trim();
        }
        return null;
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible && firstTime) {
            firstTime = false;
            
            final IRunnableContext ctx = getRunnableContext();
            
            // run asynchronously -- we allow dialog to get displayed, and initiate fetching of filters
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    if (getControl() != null && !getControl().isDisposed()) {
                        initializePage(ctx);
                    }
                }
            });
        }

        if (visible && getSearchContainer() != null) {
            getSearchContainer().setPerformActionEnabled(isPageComplete());
        }
    }

    private IRunnableContext getRunnableContext() {
        ITaskSearchPageContainer sc = getSearchContainer();
        if (sc != null) {
            return sc.getRunnableContext();
        }
        
        return getContainer();
    }

    private void initializePage(IRunnableContext runnableContext) {
        final FoglynRepositoryConnector connector = (FoglynRepositoryConnector) TasksUi.getRepositoryManager().getRepositoryConnector(FoglynCorePlugin.CONNECTOR_KIND);

        final AtomicReference<FogBugzClient> clientRef = new AtomicReference<FogBugzClient>(null);
        
        try {
            IRunnableWithProgress runnable = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Fetching repository details", IProgressMonitor.UNKNOWN);
                        FogBugzClient client = connector.getClientManager().getFogbugzClient(getTaskRepository(), monitor);
                        
                        clientRef.set(client);
                        
                        monitor.done();
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e, e.getMessage());
                    }
                }
            };
    
            runnableContext.run(true, true, runnable);
        } catch (InvocationTargetException e) {
            FoglynUIPlugin.log("Cannot fetch repository data from FogBugz", e);
            setErrorMessage("Cannot fetch repository data from FogBugz");
        } catch (InterruptedException e) {
            FoglynUIPlugin.log("Cannot fetch repository data from FogBugz", e);
            setErrorMessage("Cannot fetch repository data from FogBugz");
        }

        FogBugzClient client = clientRef.get();
        if (client != null) {
            setupComponentsFromClient(client);
        }
        
        if (inSearchContainer()) {
            restoreState();
        } else if (initialQuery != null) {
            setupComponentsFromInitialQuery();
        }
    }

    private void updateRepositoryConfiguration() {
        final IRunnableContext ctx = getRunnableContext();
        
        // run asynchronously -- we allow dialog to get displayed, and initiate fetching of filters
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                if (getControl() != null && !getControl().isDisposed()) {
                    updateRepositoryValues(ctx);
                }
            }
        });
    }
    
    private void updateRepositoryValues(IRunnableContext runnableContext) {
        final FoglynRepositoryConnector connector = (FoglynRepositoryConnector) TasksUi.getRepositoryManager().getRepositoryConnector(FoglynCorePlugin.CONNECTOR_KIND);

        final AtomicReference<FogBugzClient> clientRef = new AtomicReference<FogBugzClient>(null);
        
        try {
            IRunnableWithProgress runnable = new IRunnableWithProgress() {
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask("Updating repository configuration", IProgressMonitor.UNKNOWN);
                        connector.updateRepositoryConfiguration(getTaskRepository(), monitor);
                        
                        FogBugzClient client = connector.getClientManager().getFogbugzClient(getTaskRepository(), monitor);
                        
                        clientRef.set(client);
                        
                        monitor.done();
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e, e.getMessage());
                    }
                }
            };
    
            runnableContext.run(true, true, runnable);
        } catch (InvocationTargetException e) {
            FoglynUIPlugin.log("Cannot fetch repository data from FogBugz", e);
            setErrorMessage("Cannot fetch repository data from FogBugz");
        } catch (InterruptedException e) {
            FoglynUIPlugin.log("Cannot fetch repository data from FogBugz", e);
            setErrorMessage("Cannot fetch repository data from FogBugz");
        }

        FogBugzClient client = clientRef.get();
        if (client != null) {
            setupComponentsFromClient(client);
        }
    }
    
    private void setupComponentsFromClient(FogBugzClient client) {
        Collection<FogBugzPerson> people = client.getAllPeople();

        assignedTo.setComboValuesFromClientValues(people);
        openedBy.setComboValuesFromClientValues(people);
        closedBy.setComboValuesFromClientValues(people);
        resolvedBy.setComboValuesFromClientValues(people);
        editedBy.setComboValuesFromClientValues(people);
        lastEditedBy.setComboValuesFromClientValues(people);
        alsoEditedBy.setComboValuesFromClientValues(people);
        resolvedBy.setComboValuesFromClientValues(people);
        
        category.setComboValuesFromClientValues(client.getAllCategories());
        project.setComboValuesFromClientValues(client.getAllProjects());
        area.setComboValuesFromClientValues(client.getAllAreas());
        fixFor.setComboValuesFromClientValues(client.getAllFixFors());
        priority.setComboValuesFromClientValues(client.getAllPriorities());

        status.setStatusComboFromClientValues(client);
    }

    private void setupComponentsFromInitialQuery() {
        if (queryTitle != null) {
            queryTitle.setText(initialQuery.getQueryTitle());
        }

        project.selectOption(initialQuery.getProject());
        area.selectOption(initialQuery.getArea());
        category.selectOption(initialQuery.getCategory());
        status.selectStatuses(initialQuery.getStatuses());
        priority.selectOption(initialQuery.getPriority());
        fixFor.selectOption(initialQuery.getFixFor());
        
        openedBy.selectOption(initialQuery.getOpenedBy());
        opened.selectDateCondition(initialQuery.getOpened());
        
        editedBy.selectOption(initialQuery.getEditedBy());
        lastEditedBy.selectOption(initialQuery.getLastEditedBy());
        alsoEditedBy.selectOption(initialQuery.getAlsoEditedBy());
        edited.selectDateCondition(initialQuery.getEdited());

        resolvedBy.selectOption(initialQuery.getResolvedBy());
        resolved.selectDateCondition(initialQuery.getResolved());

        closedBy.selectOption(initialQuery.getClosedBy());
        closed.selectDateCondition(initialQuery.getClosed());

        assignedTo.selectOption(initialQuery.getAssignedTo());

        due.selectDateCondition(initialQuery.getDue());

        if (initialQuery.getSearchString() != null) {
            searchQuery.setText(initialQuery.getSearchString());
        }
        
        if (initialQuery.getCorrespondent() != null) {
            correspondent.setText(initialQuery.getCorrespondent());
        }

        starredByMe.setSelection(initialQuery.getStarredByMe());
        subscribed.setSelection(initialQuery.getSubscribedByMe());
        
        if (isFogBugz7Repo && tags != null) {
            tags.setText(initialQuery.getTags());
        }
    }
    
    private void addSeparator(Composite panel)
    {
        Label s = new Label(panel, SWT.SEPARATOR | SWT.HORIZONTAL);
        s.setLayoutData("growx, span");
    }

    private void refreshViewer(StructuredViewer viewer) {
        IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
        
        viewer.refresh();
        viewer.setSelection(sel);
        
        IStructuredSelection after = (IStructuredSelection) viewer.getSelection();
        if (after.isEmpty()) {
            viewer.setSelection(HelperConstants.NULL_VALUE_SELECTION);
        }
    }

    private void configureCombo(ComboViewer combo) {
        combo.setInput(Collections.emptyList());
        combo.setSelection(HelperConstants.NULL_VALUE_SELECTION);
        combo.getCombo().setVisibleItemCount(Constants.NUMBER_OF_ENTRIES_IN_COMBOBOX);
        combo.getCombo().setLayoutData("width 10%:pref:50%");
        combo.addSelectionChangedListener(searchOptionsListener);
    }
    
    private <K extends ID, T extends HasID<K>> FBDatatypeComboViewer<K, T> createFBDatatypeCombo(Class<T> clazz, Composite parent, String nullValueLabel, ILabelProvider labelProvider) {
        FBDatatypeComboViewer<K, T> combo = FBDatatypeComboViewer.create(clazz, parent, SWT.DROP_DOWN | SWT.READ_ONLY, nullValueLabel, labelProvider);
        configureCombo(combo);
        return combo;
    }
    
    private FBDatatypeComboViewer<PersonID, FogBugzPerson> createPeopleCombo(Composite parent) {
        return createFBDatatypeCombo(FogBugzPerson.class, parent, "\u2014 Anybody \u2014", new PersonLabelProvider());
    }
    
    private DateConditionComboViewer createDateConditionCombo(Composite control, List<DateCondition> conditions) {
        DateConditionComboViewer combo = DateConditionComboViewer.create(control, SWT.DROP_DOWN | SWT.READ_ONLY, "\u2014 Any time \u2014");
        configureCombo(combo);
        combo.setDateConditionsInput(conditions);
        return combo;
    }

    private FBStatusesComboViewer createFBStatusesCombo(Composite parent, String nullValueLabel) {
        FBStatusesComboViewer combo = new FBStatusesComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY, nullValueLabel);
        configureCombo(combo);
        return combo;
    }
    
    private Text createTextField(Composite control) {
        Text text = new Text(control, SWT.SINGLE | SWT.BORDER);
        text.addModifyListener(searchOptionsListener);
        return text;
    }

    private Button createCheckbox(Composite control, String text) {
        Button button = new Button(control, SWT.CHECK);
        button.setText(text);
        button.setLayoutData("skip");
        
        button.addSelectionListener(searchOptionsListener);
        
        return button;
    }
    
    private class SearchPageChangedListener implements ISelectionChangedListener, ModifyListener, SelectionListener {
        public void selectionChanged(SelectionChangedEvent event) {
            refresh();
        }

        public void modifyText(ModifyEvent e) {
            refresh();
        }

        public void widgetDefaultSelected(SelectionEvent e) {
            refresh();
        }

        public void widgetSelected(SelectionEvent e) {
            refresh();
        }

        private void refresh() {
            if (isControlCreated()) {
                setPageComplete(isPageComplete());
            }
        }
    }
    
    @Override
    public void setControlsEnabled(boolean enabled) {
        this.controlsEnabled = enabled;

        super.setControlsEnabled(enabled);

        if (enabled) {
            if (editedBy.getSelectedID() != null) {
                enableAlsoEditedBy(true);
            } else {
                enableAlsoEditedBy(false);
            }
        }
    }

    @Override
    public boolean performSearch() {
        if (inSearchContainer()) {
            saveState();
        }
        
        return super.performSearch();
    }
    
    @Override
    public void saveState() {
        String repoID = "." + getTaskRepository().getRepositoryUrl();
        
        IDialogSettings dlgSettings = getDialogSettings();
        
        dlgSettings.put(SEARCH_QUERY + repoID, getText(searchQuery));
        saveID(dlgSettings, ASSIGNED_TO + repoID, assignedTo.getSelectedID());
        saveID(dlgSettings, RESOLVED_BY + repoID, resolvedBy.getSelectedID());
        saveID(dlgSettings, OPENED_BY + repoID, openedBy.getSelectedID());
        saveID(dlgSettings, EDITED_BY + repoID, editedBy.getSelectedID());

        PersonID alsoEditedByID = null;
        if (editedBy.getSelectedID() != null) {
            alsoEditedByID = alsoEditedBy.getSelectedID();
        }
        saveID(dlgSettings, ALSO_EDITED_BY + repoID, alsoEditedByID);
        
        saveID(dlgSettings, LAST_EDITED_BY + repoID, lastEditedBy.getSelectedID());
        saveID(dlgSettings, CLOSED_BY + repoID, closedBy.getSelectedID());
        saveID(dlgSettings, CATEGORY + repoID, category.getSelectedID());
        saveID(dlgSettings, PROJECT + repoID, project.getSelectedID());
        saveID(dlgSettings, AREA + repoID, area.getSelectedID());
        saveIDs(dlgSettings, STATUS + repoID, status.getSelectedStatuses());
        saveID(dlgSettings, FIXFOR + repoID, fixFor.getSelectedID());
        saveID(dlgSettings, PRIORITY + repoID, priority.getSelectedID());
        
        saveDateCondition(dlgSettings, OPENED_DATE + repoID, opened.getDateCondition());
        saveDateCondition(dlgSettings, EDITED_DATE + repoID, edited.getDateCondition());
        saveDateCondition(dlgSettings, RESOLVED_DATE + repoID, resolved.getDateCondition());
        saveDateCondition(dlgSettings, CLOSED_DATE + repoID, closed.getDateCondition());
        saveDateCondition(dlgSettings, DUE_DATE + repoID, due.getDateCondition());
        
        dlgSettings.put(CORRESPONDENT + repoID, getText(correspondent));
        dlgSettings.put(STARRED_BY_ME + repoID, starredByMe.getSelection());
        dlgSettings.put(SUBSCRIBED + repoID, subscribed.getSelection());
        
        if (isFogBugz7Repo && tags != null) {
            dlgSettings.put(TAGS + repoID, getText(tags));
        }
    }

    public void restoreState() {
        String repoID = "." + getTaskRepository().getRepositoryUrl();
        
        IDialogSettings dlgSettings = getDialogSettings();

        project.selectOption(getID(dlgSettings, PROJECT + repoID, new ProjectIDFactory()));
        area.selectOption(getID(dlgSettings, AREA + repoID, new AreaIDFactory()));
        category.selectOption(getID(dlgSettings, CATEGORY + repoID, new CategoryIDFactory()));
        status.selectStatuses(getIDs(dlgSettings, STATUS + repoID, new StatusIDFactory()));
        priority.selectOption(getID(dlgSettings, PRIORITY + repoID, new PriorityIDFactory()));
        fixFor.selectOption(getID(dlgSettings, FIXFOR + repoID, new FixForIDFactory()));
        
        PersonIDFactory pidf = new PersonIDFactory();
        
        openedBy.selectOption(getID(dlgSettings, OPENED_BY + repoID, pidf));
        opened.selectDateCondition(getDateCondition(dlgSettings, OPENED_DATE + repoID));
        
        editedBy.selectOption(getID(dlgSettings, EDITED_BY + repoID, pidf));
        lastEditedBy.selectOption(getID(dlgSettings, LAST_EDITED_BY + repoID, pidf));
        alsoEditedBy.selectOption(getID(dlgSettings, ALSO_EDITED_BY + repoID, pidf));
        edited.selectDateCondition(getDateCondition(dlgSettings, EDITED_DATE + repoID));

        resolvedBy.selectOption(getID(dlgSettings, RESOLVED_BY + repoID, pidf));
        resolved.selectDateCondition(getDateCondition(dlgSettings, RESOLVED_DATE + repoID));

        closedBy.selectOption(getID(dlgSettings, CLOSED_BY + repoID, pidf));
        closed.selectDateCondition(getDateCondition(dlgSettings, CLOSED_DATE + repoID));

        assignedTo.selectOption(getID(dlgSettings, ASSIGNED_TO + repoID, pidf));

        due.selectDateCondition(getDateCondition(dlgSettings, DUE_DATE + repoID));

        if (dlgSettings.get(SEARCH_QUERY + repoID) != null) {
            searchQuery.setText(dlgSettings.get(SEARCH_QUERY + repoID));
        }
        
        if (dlgSettings.get(CORRESPONDENT + repoID) != null) {
            correspondent.setText(dlgSettings.get(CORRESPONDENT + repoID));
        }

        starredByMe.setSelection(dlgSettings.getBoolean(STARRED_BY_ME + repoID));
        subscribed.setSelection(dlgSettings.getBoolean(SUBSCRIBED + repoID));
        
        if (isFogBugz7Repo && tags != null) {
            if (dlgSettings.get(TAGS + repoID) != null) {
                tags.setText(dlgSettings.get(TAGS + repoID));
            }
        }
    }
    
    private DateCondition getDateCondition(IDialogSettings dlgSettings, String string) {
        String value = dlgSettings.get(string);
        if (com.foglyn.core.Utils.isEmpty(value)) {
            return null;
        }
        
        return DateCondition.valueOf(value);
    }

    private Set<StatusID> getIDs(IDialogSettings dlgSettings, String string, StatusIDFactory statusIDFactory) {
        String[] values = dlgSettings.getArray(string);
        if (values == null) {
            return null;
        }
     
        Set<StatusID> result = new HashSet<StatusID>();
        
        for (String s: values) {
            if (!com.foglyn.core.Utils.isEmpty(s)) {
                result.add(statusIDFactory.valueOf(s));
            }
        }
        
        return result;
    }

    private <T extends ID> T getID(IDialogSettings dlgSettings, String string, IDFactory<T> idFactory) {
        String value = dlgSettings.get(string);
        if (com.foglyn.core.Utils.isEmpty(value)) {
            return null;
        }
        
        return idFactory.valueOf(value);
    }

    private void saveDateCondition(IDialogSettings dlgSettings, String setting, DateCondition dateCondition) {
        if (dateCondition == null) {
            dlgSettings.put(setting, (String) null);
        } else {
            dlgSettings.put(setting, dateCondition.name());
        }
    }

    private void saveIDs(IDialogSettings dlgSettings, String setting, Set<StatusID> selectedStatuses) {
        if (selectedStatuses == null || selectedStatuses.isEmpty()) {
            dlgSettings.put(setting, (String) null);
        } else {
            dlgSettings.put(setting, com.foglyn.core.Utils.toStrings(selectedStatuses).toArray(new String[0]));
        }
    }

    private void saveID(IDialogSettings dlgSettings, String setting, ID selectedID) {
        if (selectedID == null) {
            dlgSettings.put(setting, (String) null);
        } else {
            dlgSettings.put(setting, selectedID.toString());
        }
    }

    private void resetWidgetsToDefaults() {
        project.selectOption(null);
        area.selectOption(null);
        category.selectOption(null);
        status.selectStatuses(null);
        priority.selectOption(null);
        fixFor.selectOption(null);
        
        openedBy.selectOption(null);
        opened.selectDateCondition(null);
        
        editedBy.selectOption(null);
        lastEditedBy.selectOption(null);
        alsoEditedBy.selectOption(null);
        edited.selectDateCondition(null);

        resolvedBy.selectOption(null);
        resolved.selectDateCondition(null);

        closedBy.selectOption(null);
        closed.selectDateCondition(null);

        assignedTo.selectOption(null);

        due.selectDateCondition(null);
        
        searchQuery.setText("");
        correspondent.setText("");

        starredByMe.setSelection(false);
        subscribed.setSelection(false);
        
        if (isFogBugz7Repo && tags != null) {
            tags.setText("");
        }
    }
    
    @Override
    protected IDialogSettings getDialogSettings() {
        IDialogSettings settings = FoglynUIPlugin.getDefault().getDialogSettings();
        IDialogSettings dialogSettings = settings.getSection(PAGE_NAME);
        if (dialogSettings == null) {
            dialogSettings = settings.addNewSection(PAGE_NAME);
        }
        return dialogSettings;
    }

    void setQueryTitle(String queryTitle) {
        this.initialQueryName = queryTitle;
    }
}
