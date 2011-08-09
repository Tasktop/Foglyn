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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.ui.handlers.HandlerUtil;

abstract class AbstractAttachmentHandler extends AbstractHandler {
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }

        List<ITaskAttachment> attachments = new ArrayList<ITaskAttachment>();

        List<?> items = ((IStructuredSelection) selection).toList();
        for (Object o : items) {
            if (!(o instanceof ITaskAttachment)) {
                continue;
            }

            attachments.add((ITaskAttachment) o);
        }

        if (attachments.isEmpty()) {
            return null;
        }
        
        return handleAttachments(event, attachments);
    }

    abstract protected Object handleAttachments(ExecutionEvent event, List<ITaskAttachment> attachments);
}
