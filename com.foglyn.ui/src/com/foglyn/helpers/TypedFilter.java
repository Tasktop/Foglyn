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
