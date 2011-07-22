package com.foglyn.helpers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filter which handles {@link HelperConstants#NULL_VALUE} element.
 */
public class NullValueFilter extends ViewerFilter {
    private ViewerFilter filter;
    
    public NullValueFilter(ViewerFilter filter) {
        this.filter = filter;
    }
    
    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (HelperConstants.NULL_VALUE.equals(element)) {
            return true;
        }
        
        return filter.select(viewer, parentElement, element);
    }
}
