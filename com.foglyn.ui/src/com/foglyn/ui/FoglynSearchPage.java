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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.foglyn.core.SimpleSearchQuery;

public class FoglynSearchPage extends AbstractRepositoryQueryPage {
    private Text queryText;

    public FoglynSearchPage(String title, TaskRepository taskRepository) {
        super(title, taskRepository);
    }

    @Override
    public String getQueryTitle() {
        return queryText.getText();
    }

    public void createControl(Composite parent) {
        Composite control = new Composite(parent, SWT.NO_SCROLL);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(control);
        
        Label queryNameLabel = new Label(control, SWT.RIGHT);
        queryNameLabel.setText("Search:");
        
        queryText = new Text(control, SWT.SINGLE | SWT.BORDER);
        
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(control); 
        
        GridDataFactory.fillDefaults().align(SWT.TRAIL, SWT.CENTER).applyTo(queryNameLabel);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(queryText);
        
        setControl(control);
    }
    
    // Don't forget to enable/disable buttons if we are complete.
    @Override
    public void setPageComplete(boolean complete) {
        super.setPageComplete(complete);
        
        if (getSearchContainer() != null) {
            getSearchContainer().setPerformActionEnabled(complete);
        }
    }
    
    @Override
    public boolean isPageComplete() {
        setErrorMessage(null);
        return true;
    }

    @Override
    public void applyTo(IRepositoryQuery query) {
        SimpleSearchQuery ssq = new SimpleSearchQuery();
        
        ssq.setQueryTitle("Search query: " + queryText.getText());
        ssq.setSearchString(queryText.getText());

        ssq.saveToQuery(query);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        if (visible) {
            getSearchContainer().setPerformActionEnabled(isPageComplete());
        }
    }
}
