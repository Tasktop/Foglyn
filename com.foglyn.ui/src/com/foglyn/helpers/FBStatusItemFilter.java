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

import org.eclipse.jface.viewers.Viewer;

import com.foglyn.fogbugz.FogBugzCategory.CategoryID;

class FBStatusItemFilter extends TypedFilter<FBStatusItem> {
    private CategoryID categoryID;
    
    FBStatusItemFilter() {
        super(FBStatusItem.class);
    }
    
    void setCategoryID(CategoryID categoryID) {
        this.categoryID = categoryID;
    }
    
    @Override
    public boolean selectElement(Viewer viewer, Object parentElement, FBStatusItem val) {
        if (categoryID == null) {
            return true;
        }
        
        if (val.getCategoryID() == null) {
            return true;
        }
        
        return categoryID.equals(val.getCategoryID());
    }
}

