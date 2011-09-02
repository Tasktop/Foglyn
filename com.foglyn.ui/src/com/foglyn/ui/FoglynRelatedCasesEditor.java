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

import java.util.List;

import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class FoglynRelatedCasesEditor extends AbstractAttributeEditor {

    public FoglynRelatedCasesEditor(TaskDataModel model, TaskAttribute taskAttribute) {
        super(model, taskAttribute);
    }

    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        List<String> values = getTaskAttribute().getValues();
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("<form><p>");
        
        boolean first = true;
        for (String caseID: values) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            
            sb.append("<a href=\"");
            sb.append(caseID);
            sb.append("\">");
            sb.append(caseID);
            sb.append("</a>");
        }
        
        sb.append("</p></form>");

        FormText formText = toolkit.createFormText(parent, true);
        formText.setText(sb.toString(), true, false);
        formText.addHyperlinkListener(new OpenTask(getModel().getTaskRepository()));
        
        setControl(formText);
    }
    
    static class OpenTask extends HyperlinkAdapter {
        private final TaskRepository repository;

        OpenTask(TaskRepository repos) {
            this.repository = repos;
        }
        
        @Override
        public void linkActivated(HyperlinkEvent e) {
            if (e.getHref() != null) {
                TasksUiUtil.openTask(repository, String.valueOf(e.getHref()));
            }
        }
    }
}
