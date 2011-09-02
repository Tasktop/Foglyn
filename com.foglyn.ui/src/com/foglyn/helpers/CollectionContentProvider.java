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
