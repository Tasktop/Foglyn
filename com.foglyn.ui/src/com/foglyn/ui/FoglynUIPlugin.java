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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.mylyn.tasks.ui.TaskRepositoryLocationUiFactory;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

import com.foglyn.core.FoglynCorePlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class FoglynUIPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "com.foglyn.ui";

    // Moved to repository property instead. See FoglynConstants#REPOSITORY_SYNCHRONIZE_WORKING_ON
    @Deprecated
    private static final String WORKING_ON_SYNC_ENABLED_OPTION = "workingOnSynchronizationEnabled";
    
    private static final AtomicReference<FoglynUIPlugin> plugin = new AtomicReference<FoglynUIPlugin>();
    
    private final AtomicReference<FoglynTaskActivationListener> activationListener = new AtomicReference<FoglynTaskActivationListener>();
    
    private volatile int focusWidth;
    
    @Deprecated
    private final AtomicBoolean workingOnSynchronizationEnabled = new AtomicBoolean(true);

    @Override
	public void start(BundleContext context) throws Exception {
        super.start(context);
        
        FoglynUIPlugin.plugin.compareAndSet(null, this);
        
        FoglynCorePlugin.getDefault().getClientFactory().setTaskRepositoryLocationFactory(new TaskRepositoryLocationUiFactory());

        if (activationListener.get() == null) {
            FoglynTaskActivationListener listener = new FoglynTaskActivationListener();
            if (activationListener.compareAndSet(null, listener)) {
                TasksUi.getTaskActivityManager().addActivationListener(listener);
            }
        }
        
        focusWidth = computeFocusWidth();
        
        boolean woSyncEnabled = Boolean.parseBoolean(loadOption(WORKING_ON_SYNC_ENABLED_OPTION, "true"));
        this.workingOnSynchronizationEnabled.set(woSyncEnabled);
    }
    
    private int computeFocusWidth() {
        String os = getBundle().getBundleContext().getProperty("org.osgi.framework.os.name");
        if (os.startsWith("Mac")) {
            return 5;
        } else {
            return 1;
        }
    }

    @Override
	public void stop(BundleContext context) throws Exception {
        FoglynUIPlugin.plugin.compareAndSet(this, null);
        
        FoglynTaskActivationListener listener = activationListener.getAndSet(null);
        if (listener != null) {
            TasksUi.getTaskActivityManager().removeActivationListener(listener);
        }

        super.stop(context);
    }

    public static FoglynUIPlugin getDefault() {
        return plugin.get();
    }
    
    public static void log(String message, Throwable e) {
        getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
    }
    
    public static String loadOption(String option, String defaultValue) {
        IEclipsePreferences prefs = new InstanceScope().getNode(FoglynUIPlugin.PLUGIN_ID);
        return prefs.get(option, defaultValue);
    }
    
    public static void saveOption(String option, String value) {
        IEclipsePreferences prefs = new InstanceScope().getNode(FoglynUIPlugin.PLUGIN_ID);
        prefs.put(option, value);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            // ignore
        }
    }

    int getFocussWidth() {
        return focusWidth;
    }

    @Deprecated
    public boolean isWorkingOnSynchronizationEnabled() {
        return workingOnSynchronizationEnabled.get();
    }
}
