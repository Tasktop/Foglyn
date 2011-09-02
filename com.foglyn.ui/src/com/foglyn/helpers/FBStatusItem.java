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

package com.foglyn.helpers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;

class FBStatusItem {
    private final String name;
    private final String prefix;

    private final Set<StatusID> statuses;
    private final CategoryID category;
    
    FBStatusItem(String name, String prefix, StatusID status, CategoryID category) {
        this(name, prefix, Collections.singleton(status), category);
    }

    FBStatusItem(String name, String prefix, Collection<StatusID> statuses, CategoryID category) {
        this.name = name;
        this.prefix = prefix;
        this.statuses = new HashSet<StatusID>(statuses);
        this.category = category;
    }

    CategoryID getCategoryID() {
        return category;
    }
    
    String getPrefix() {
        return prefix;
    }
    
    String getName() {
        return name;
    }

    Set<StatusID> getStatuses() {
        return statuses;
    }
    
    @Override
    public String toString() {
        if (prefix == null) {
            return name + " " + statuses.toString();
        } else {
            return prefix + ": " + name + " " + statuses.toString();
        }
    }
}
