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

import org.eclipse.mylyn.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPageFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;

import com.foglyn.core.FoglynCorePlugin;

public class FoglynTaskEditorPageFactory extends AbstractTaskEditorPageFactory {

    @Override
    public boolean canCreatePageFor(TaskEditorInput input) {
        if (input.getTask().getConnectorKind().equals(FoglynCorePlugin.CONNECTOR_KIND)
                || TasksUiUtil.isOutgoingNewTask(input.getTask(), FoglynCorePlugin.CONNECTOR_KIND)) {
            return true;
        }
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IFormPage createPage(TaskEditor parentEditor) {
//        return new BrowserFormPage(parentEditor, "FogBugz");
        return new FoglynTaskEditorPage(parentEditor);
    }

    @Override
    public Image getPageImage() {
        return FoglynImages.getImage(TasksUiImages.REPOSITORY_SMALL);
    }

    @Override
    public int getPriority() {
        return PRIORITY_TASK;
    }
    
    @Override
    public String getPageText() {
        return "FogBugz";
    }

    @Override
    public String[] getConflictingIds(TaskEditorInput input) {
        return new String[] { ITasksUiConstants.ID_PAGE_PLANNING };
    }
}
