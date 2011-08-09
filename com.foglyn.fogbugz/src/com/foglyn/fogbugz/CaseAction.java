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

public enum CaseAction {
    NEW("new"),
    EDIT("edit"),
    ASSIGN("assign"),
    REACTIVATE("reactivate"),
    REOPEN("reopen"),
    RESOLVE("resolve"),
    CLOSE("close"),
    EMAIL("email"),
    REPLY("reply"),
    FORWARD("forward");
    
    private final String fogbugzCommand;
    
    CaseAction(String cmd) {
        this.fogbugzCommand = cmd;
    }
    
    String getCommand() {
        return fogbugzCommand;
    }

    static CaseAction getActionFromCommand(String a) {
        for (CaseAction action: values()) {
            if (action.getCommand().equals(a)) {
                return action;
            }
        }
        
        return null;
    }
}
