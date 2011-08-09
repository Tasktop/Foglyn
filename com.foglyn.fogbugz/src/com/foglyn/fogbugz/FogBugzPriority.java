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


/**
 * Priority of case. FogBugz has 7 priority level, which have configurable
 * names.
 * 
 * Note: instances of this class are immutable.
 */
public final class FogBugzPriority implements HasID<FogBugzPriority.PriorityID> {
    public final static class PriorityID extends LongID {
        private PriorityID(long priorityID) {
            super(priorityID);
        }

        public static PriorityID valueOf(String ixPriority) {
            return new PriorityID(Long.parseLong(ixPriority));
        }
    }

    public static class PriorityIDFactory implements IDFactory<PriorityID> {
        public PriorityID valueOf(String ixPriority) {
            return PriorityID.valueOf(ixPriority);
        }
    }
    
    private final PriorityID priorityID;
    private final String name;
    private final boolean defaultPriority;

    FogBugzPriority(PriorityID priorityID, String name, boolean isDefault) {
        Utils.assertNotNullArg(priorityID, "PriorityID");
        Utils.assertNotNullArg(name, "name");
        
        this.priorityID = priorityID;
        this.name = name;
        this.defaultPriority = isDefault;
    }

    /**
     * @return true if this is priority, which should be pre-selected when
     *         creating new case.
     */
    public boolean isDefaultPriority() {
        return defaultPriority;
    }

    public PriorityID getID() {
        return priorityID;
    }

    /**
     * @return priority name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Priority: " + name + " (" + priorityID + ")";
    }

    public String toUserString() {
        return priorityID.toString() + " \u2013 " + name;
    }
}
