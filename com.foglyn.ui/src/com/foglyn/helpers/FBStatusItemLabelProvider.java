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
