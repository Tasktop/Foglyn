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

/**
 * Repository setting which says what cases are treated as complete.
 */
public enum CompletedCaseMode {
    /**
     * Case must be closed to be "completed".
     */
    CLOSED("closed"),
    
    /**
     * Case must be resolved or closed to be "completed".
     */
    RESOLVED_OR_CLOSED("resolved"),
    
    /**
     * Case is "completed" when it is resolved or closed.
     * If case is assigned to current user, it must be closed.
     */
    SMART_RESOLVED_OR_CLOSED("smart_resolved");
    
    private final String id;
    
    private CompletedCaseMode(String id) {
        this.id = id;
    }
    
    String getID() {
        return id;
    }
    
    static CompletedCaseMode getByID(String id) {
        for (CompletedCaseMode ccm: values()) {
            if (ccm.id.equals(id)) {
                return ccm;
            }
        }
        
        return null;
    }
}
