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

