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

package com.foglyn.fogbugz;

import java.util.Comparator;

import com.foglyn.fogbugz.FogBugzCategory.CategoryID;

/**
 * Status of case, like 'Active', 'Resolved (fixed)' or 'Closed'.
 * 
 * Some statuses are valid for some category only.
 * 
 * Note: instances of this class are immutable.
 */
public final class FogBugzStatus implements HasID<FogBugzStatus.StatusID> {
    public final static class StatusID extends LongID {
        private StatusID(long statusID) {
            super(statusID);
        }

        public static StatusID valueOf(String ixStatus) {
            return new StatusID(Long.parseLong(ixStatus));
        }
    }
    
    public static class StatusIDFactory implements IDFactory<StatusID> {
        public StatusID valueOf(String ixStatus) {
            return StatusID.valueOf(ixStatus);
        }
    }

    private final StatusID statusID;
    private final String name;
    private final CategoryID categoryID;
    private final boolean resolved;
    private final int order;

    FogBugzStatus(StatusID statusID, String name, CategoryID categoryID, boolean resolved, int order) {
        Utils.assertNotNullArg(statusID, "statusID");
        Utils.assertNotNullArg(name, "name");
        // categoryID can be null.
        
        this.statusID = statusID;
        this.name = name;
        this.categoryID = categoryID;
        this.resolved = resolved;
        this.order = order;
    }

    public StatusID getID() {
        return statusID;
    }

    /**
     * @return name of status
     */
    public String getName() {
        return name;
    }

    /**
     * @return ID of {@link FogBugzCategory category}, for which this status is
     *         applicable, or <code>null</code> if this status is valid for all
     *         categories.
     */
    public CategoryID getCategoryID() {
        return categoryID;
    }

    public boolean isResolved() {
        return resolved;
    }
    
    /**
     * Order was introduced in FogBugz 7, and it specifies relative position of status compared to other statuses.
     * FogBugz uses orders 0 and bigger. If no order was supplied by server (i.e. FogBugz 6 server), order will be negative.
     */
    public int getOrder() {
        return order;
    }
    
    @Override
    public String toString() {
        return "Status: " + name + " (" + statusID + "), resolved=" + resolved + ", order=" + order;
    }
    
    public static class FogBugzStatusOrderComparator implements Comparator<FogBugzStatus> {
        public int compare(FogBugzStatus s1, FogBugzStatus s2) {
            int o1 = s1.getOrder();
            int o2 = s2.getOrder();
            
            if (o1 < o2) {
                return -1;
            } else if (o1 > o2) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
