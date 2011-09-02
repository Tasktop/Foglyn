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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenAttachmentWithDefaultProgramHandler extends AbstractAttachmentHandler {
    @Override
    protected Object handleAttachments(ExecutionEvent event, List<ITaskAttachment> attachments) {
        final Shell shell = HandlerUtil.getActiveShell(event);
        if (shell == null) {
            return null;
        }

        for (ITaskAttachment a: attachments) {
            openAttachments(shell, a);
        }
        
        return null;
    }

    private void openAttachments(final Shell shell, final ITaskAttachment attachment) {
        String extension = getAttachmentExtension(attachment);
        
        final Program program = Program.findProgram(extension);
        if (program == null) {
            MessageDialog.openError(shell, "Unable to find default program", "Cannot find default program associated with " + attachment.getFileName());
            return;
        }
        
        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                String filename = "unknown.bin";
                
                if (attachment.getFileName() != null) {
                    filename = attachment.getFileName();
                }

                File file = null;
                try {
                    file = File.createTempFile("attach-", "-" + filename);
                } catch (IOException e) {
                    throw new InvocationTargetException(e);
                }
                file.deleteOnExit();

                monitor.beginTask("Downloading attachment " + filename, IProgressMonitor.UNKNOWN);
                
                try {
                    AttachmentUtils.download(attachment, monitor, file);
                } catch (IOException e) {
                    AttachmentUtils.delete(file);
                    throw new InvocationTargetException(e);
                } catch (CoreException e) {
                    AttachmentUtils.delete(file);
                    throw new InvocationTargetException(e);
                }

                Program program = Program.findProgram(getAttachmentExtension(attachment));
                if (program == null) {
                    throw new InvocationTargetException(new CoreException(new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, "Cannot find default program associated with " + attachment.getFileName())));
                }
                
                // everything is OK... invoke
                if (!program.execute(file.getAbsolutePath())) {
                    throw new InvocationTargetException(new CoreException(new Status(IStatus.ERROR, FoglynUIPlugin.PLUGIN_ID, "Unable to start " + program.getName())));
                }
            }
        };
        
        ProgressMonitorDialog dlg = new ProgressMonitorDialog(shell);
        try {
            dlg.run(true, true, runnable);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            
            if (cause instanceof IOException) {
                MessageDialog.openError(shell, "Unable to open attachment in default program", "Input/output problem occured: " + cause.getMessage());
            } else {
                MessageDialog.openError(shell, "Unable to open attachment in default program", cause.getMessage());
            }
        } catch (InterruptedException e) {
            //
        }
    }

    private String getAttachmentExtension(final ITaskAttachment attachment) {
        String filename = attachment.getFileName();
        if (filename != null) {
            int last = filename.lastIndexOf('.');
            if (last >= 0) {
                return filename.substring(last);
            }
        }
        return ".bin";
    }
}
