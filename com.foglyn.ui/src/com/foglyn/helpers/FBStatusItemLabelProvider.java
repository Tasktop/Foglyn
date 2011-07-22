/**
 * 
 */
package com.foglyn.helpers;

public class FBStatusItemLabelProvider extends TypedLabelProvider<FBStatusItem> {
    private boolean usePrefix;
    
    FBStatusItemLabelProvider() {
        super(FBStatusItem.class);
    }
    
    void setUsePrefix(boolean usePrefix) {
        this.usePrefix = usePrefix;
    }

    @Override
    protected String getTextForElement(FBStatusItem element) {
        if (element.getPrefix() == null || !usePrefix) {
            return element.getName();
        }
        
        return element.getPrefix() + ": " + element.getName();
    }
}