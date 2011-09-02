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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.LegendElement;
import org.eclipse.mylyn.tasks.ui.TaskHyperlink;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskSearchPage;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;

import com.foglyn.core.AdvancedSearchQuery;
import com.foglyn.core.CasePatterns;
import com.foglyn.core.FilterQuery;
import com.foglyn.core.FoglynConstants;
import com.foglyn.core.FoglynCorePlugin;
import com.foglyn.core.FoglynQuery;
import com.foglyn.core.FoglynQueryFactory;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;

public class FoglynRepositoryConnectorUi extends AbstractRepositoryConnectorUi {
    @Override
    public String getConnectorKind() {
        return FoglynCorePlugin.CONNECTOR_KIND;
    }

    @Override
    public IWizard getNewTaskWizard(TaskRepository taskRepository, ITaskMapping selection) {
        return new FoglynNewTaskWizard(taskRepository, selection);
    }

    @Override
    public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery queryToEdit) {
        RepositoryQueryWizard wizard = new RepositoryQueryWizard(repository);

        FoglynQuery query = null;
        
        if (queryToEdit != null) {
            query = FoglynQueryFactory.queryInstance(queryToEdit);
        }
        
        if (query != null) {
            if (FoglynNewQueryWizardPage.isWizardQuery(queryToEdit)) {
                FoglynNewQueryWizardPage page = new FoglynNewQueryWizardPage(repository, queryToEdit, query);
                
                wizard.addPage(page);
            } else {
                if (query instanceof FilterQuery) {
                    FoglynFilterQueryPage page = new FoglynFilterQueryPage(repository, queryToEdit, (FilterQuery) query);
    
                    wizard.addPage(page);
                } else if (query instanceof AdvancedSearchQuery) {
                    FoglynAdvancedSearchPage page = new FoglynAdvancedSearchPage(repository, queryToEdit, (AdvancedSearchQuery) query);
                    
                    wizard.addPage(page);
                }
            }
        } else {
            // no existing query
            FoglynNewQueryWizardPage page = new FoglynNewQueryWizardPage(repository);
            
            wizard.addPage(page);
        }

        return wizard;
    }

    @Override
    public ITaskRepositoryPage getSettingsPage(TaskRepository taskRepository) {
        return new FoglynRepositorySettingsPage(
                "FogBugz Repository Settings",
                "Example: https://ondemand.fogbugz.com/",
                taskRepository);
    }

    @Override
    public boolean hasSearchPage() {
        return true;
    }
    
    @Override
	public ITaskSearchPage getSearchPage(TaskRepository repository, IStructuredSelection selection) {
        // return new FoglynSearchPage("FogBugz search", repository);
        return new FoglynAdvancedSearchPage(repository);
    }
    
    @Override
	public IHyperlink[] findHyperlinks(TaskRepository repository, String text, int lineOffset, int regionOffset) {
        List<IHyperlink> links = new ArrayList<IHyperlink>();

        for (Pattern p: CasePatterns.getPatterns()) {
            Matcher m = p.matcher(text);
            
            while (m.find()) {
                if (lineOffset < 0 || isInRegion(lineOffset, m)) {
                    String id = m.group(1);
                    links.add(new TaskHyperlink(determineRegion(regionOffset, m), repository, id));
                }
            }
        }
        
        if (links.isEmpty()) {
            return null;
        }
        
        return links.toArray(new IHyperlink[links.size()]);
    }

    private static boolean isInRegion(int lineOffset, Matcher m) {
        return (lineOffset >= m.start() && lineOffset <= m.end());
    }

    private static IRegion determineRegion(int regionOffset, Matcher m) {
        return new Region(regionOffset + m.start(), m.end() - m.start());
    }
    
    @Override
    public String getTaskKindLabel(ITask task) {
        return "Case";
    }

    @Override
    public List<LegendElement> getLegendElements() {
        List<LegendElement> legendItems = new ArrayList<LegendElement>();
        legendItems.add(LegendElement.createTask("Bug", FoglynImages.OVERLAY_BUG));
        legendItems.add(LegendElement.createTask("Feature", FoglynImages.OVERLAY_FEATURE));
        legendItems.add(LegendElement.createTask("Inquiry", FoglynImages.OVERLAY_INQUIRY));
        legendItems.add(LegendElement.createTask("Schedule Item", FoglynImages.OVERLAY_SCHEDULE_ITEM));
        return legendItems;
    }
    
    @Override
    public ImageDescriptor getTaskKindOverlay(ITask task) {
        String cidValue = task.getAttribute(FoglynConstants.TASK_ATTRIBUTE_CATEGORY);
        
        if (cidValue == null) {
            return null;
        }
        
        CategoryID cid = CategoryID.valueOf(cidValue);
        
        if (cid.equals(CategoryID.BUG)) {
            return FoglynImages.OVERLAY_BUG;
        } else if (cid.equals(CategoryID.FEATURE)) {
            return FoglynImages.OVERLAY_FEATURE;
        } else if (cid.equals(CategoryID.INQUIRY)) {
            return FoglynImages.OVERLAY_INQUIRY;
        } else if (cid.equals(CategoryID.SCHEDULE_ITEM)) {
            return FoglynImages.OVERLAY_SCHEDULE_ITEM;
        }
        
        return null;
    }
}
