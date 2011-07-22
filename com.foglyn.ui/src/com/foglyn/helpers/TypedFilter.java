package com.foglyn.helpers;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * ViewerFilter which accepts only instances of given class, and throws ClassCastException for different objects.
 */
public abstract class TypedFilter<T> extends ViewerFilter {
    private final Class<T> clz;
    
    public TypedFilter(Class<T> type) {
        this.clz = type;
    }
    
    @Override
    final public boolean select(Viewer viewer, Object parentElement, Object element) {
        T val = clz.cast(element);
        return selectElement(viewer, parentElement, val);
    }

    protected abstract boolean selectElement(Viewer viewer, Object parentElement, T val);
}
