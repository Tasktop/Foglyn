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
