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
