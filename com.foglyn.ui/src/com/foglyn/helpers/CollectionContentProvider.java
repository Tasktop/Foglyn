package com.foglyn.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class CollectionContentProvider implements IStructuredContentProvider {
    private final boolean supplyNullValue;
    
    public CollectionContentProvider(boolean supplyNullValue) {
        this.supplyNullValue = supplyNullValue;
    }
    
    public Object[] getElements(Object inputElement) {
        Collection<?> collection = (Collection<?>) inputElement;
        
        if (supplyNullValue) {
            List<Object> out = new ArrayList<Object>();
            out.add(HelperConstants.NULL_VALUE);
            out.addAll(collection);
            
            return out.toArray();
        } else {
            return collection.toArray();
        }
    }

    public void dispose() {
        // nothing to do
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // nothing to do
    }
}
