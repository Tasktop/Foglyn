/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/package com.foglyn.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.ui.TasksUi;

class AttachmentUtils {
    static void download(ITaskAttachment attachment, IProgressMonitor monitor, File targetFile) throws IOException, CoreException  {
        final AbstractRepositoryConnector connector = TasksUi.getRepositoryManager().getRepositoryConnector(attachment.getConnectorKind());

        AbstractTaskAttachmentHandler handler = connector.getTaskAttachmentHandler();

        IOException ioe = null;
        
        FileOutputStream ostream = null;
        InputStream istream = null;
        try {
            ostream = new FileOutputStream(targetFile);
            
            istream = handler.getContent(attachment.getTaskRepository(), attachment.getTask(), attachment.getTaskAttribute(), monitor);
            
            byte[] buffer = new byte[4096];
            int read = istream.read(buffer);
            while (read > 0) {
                ostream.write(buffer, 0, read);
                read = istream.read(buffer);
            }
        } catch (IOException e) {
            ioe = e;
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (IOException e) {
                    if (ioe == null) {
                        ioe = e;
                    }
                }
            }
            
            if (ioe != null) {
                throw ioe;
            }
        }
    }

    static void delete(File file) {
        if (file == null) return;
        
        // Do nothing... it will be deleted on JVM exit.
        file.delete();
    }
}
