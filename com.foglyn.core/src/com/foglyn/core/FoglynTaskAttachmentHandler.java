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

package com.foglyn.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentHandler;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskAttachmentSource;
import org.eclipse.mylyn.tasks.core.data.TaskAttachmentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

import com.foglyn.fogbugz.AttachmentData;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzCase.CaseID;

public class FoglynTaskAttachmentHandler extends AbstractTaskAttachmentHandler {
    private final FoglynRepositoryConnector connector;

    FoglynTaskAttachmentHandler(FoglynRepositoryConnector connector) {
        Assert.isNotNull(connector);
        
        this.connector = connector;
    }
    
    @Override
    public boolean canGetContent(TaskRepository repository, ITask task) {
        if (!FoglynCorePlugin.CONNECTOR_KIND.equals(task.getConnectorKind())) return false;
        return true;
    }

    @Override
    public boolean canPostContent(TaskRepository repository, ITask task) {
        if (!FoglynCorePlugin.CONNECTOR_KIND.equals(task.getConnectorKind())) return false;
        return true;
    }

    @Override
    public InputStream getContent(TaskRepository repository, ITask task, TaskAttribute attachmentAttribute, IProgressMonitor monitor)
            throws CoreException {
        FogBugzClient client = connector.getClientManager().getFogbugzClient(repository, monitor);

        TaskAttribute urlComponent = attachmentAttribute.getAttribute(FoglynAttribute.ATTACHMENT_URL_COMPONENT.getKey());
        
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        
        try {
            client.getAttachmentContent(urlComponent.getValue(), bytes, monitor);
        } catch (FogBugzException e) {
            StatusHandler.log(Utils.toStatus(e));
            
            throw new FoglynCoreException(e);
        }
        
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    @Override
    public void postContent(TaskRepository repository, ITask task,
            AbstractTaskAttachmentSource source, String comment,
            TaskAttribute attachmentAttribute, IProgressMonitor monitor)
            throws CoreException {
        if (!FoglynCorePlugin.CONNECTOR_KIND.equals(task.getConnectorKind())) {
            throw new CoreException(Utils.toErrorStatus("Cannot attach file (or context) to new task, please submit task without attachment", null));
        }
        
        FogBugzClient client = connector.getClientManager().getFogbugzClient(repository, monitor);
        
        StringBuilder fullComment = new StringBuilder();

        String description = getDescription(source, attachmentAttribute);
        if (Utils.nonEmpty(description)) {
            fullComment.append(description);
            fullComment.append("\n");
        }
        
        if (Utils.nonEmpty(comment)) {
            fullComment.append("\n");
            fullComment.append(comment);
        }
        
        try {
            client.postNewAttachment(CaseID.valueOf(task.getTaskId()), fullComment.toString(), new MylynAttachmentData(source, monitor), monitor);
        } catch (FogBugzException e) {
            StatusHandler.log(Utils.toStatus(e));
            
            throw new FoglynCoreException(e);
        }
    }

    private String getDescription(AbstractTaskAttachmentSource attachmentSource, TaskAttribute attachmentAttribute) {
        Assert.isNotNull(attachmentSource);

        String description = attachmentSource.getDescription();
        
        // Always use description from mapper (entered by user), if it exists.
        if (attachmentAttribute != null) {
            TaskAttachmentMapper mapper = TaskAttachmentMapper.createFrom(attachmentAttribute);
            
            if (mapper.getDescription() != null) {
                String mapDesc = Utils.stripNewLines(mapper.getDescription());
                
                // mapper.isPatch may be null, be careful here (i.e. don't use if (mapper.isPatch()) directly)
                if (Boolean.TRUE.equals(mapper.isPatch())) {
                    description = FoglynConstants.PATCH_DESCRIPTION_PREFIX + mapDesc;
                } else {
                    description = FoglynConstants.ATTACHMENT_DESCRIPTION_PREFIX + mapDesc;
                }
            }
        }
        
        return description;
    }

    public static class MylynAttachmentData extends AttachmentData {
        private final AbstractTaskAttachmentSource attachment;
        private final IProgressMonitor monitor;
        
        MylynAttachmentData(AbstractTaskAttachmentSource attachment, IProgressMonitor monitor) {
            Assert.isNotNull(attachment);
            Assert.isNotNull(monitor);
            this.attachment = attachment;
            this.monitor = monitor;
        }
        
        @Override
        public InputStream createInputStream() throws IOException {
            try {
                return attachment.createInputStream(monitor);
            } catch (CoreException e) {
                StatusHandler.log(new Status(IStatus.ERROR, FoglynCorePlugin.PLUGIN_ID, "Error submitting attachment", e));
                throw new IOException("Failed to create source stream");
            }
        }

        @Override
        public String getContentType() {
            return attachment.getContentType();
        }

        @Override
        public String getFilename() {
            return attachment.getName();
        }

        @Override
        public long getLength() {
            long length = attachment.getLength();
            
            // make sure we know correct length ... screenshots needs to be
            // saved into file to know size -- getting InputStream forces Mylyn
            // to do the save
            if (length < 0) {
                InputStream is = null;
                try {
                    is = createInputStream();
                } catch (IOException e) {
                    return -1;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
                
                length = attachment.getLength();
            }
            
            return length;
        }
    }
}
