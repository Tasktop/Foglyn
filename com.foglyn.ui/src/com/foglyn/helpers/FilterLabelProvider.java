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

import com.foglyn.fogbugz.FogBugzFilter;
import com.foglyn.fogbugz.FogBugzFilter.FilterType;

public class FilterLabelProvider extends TypedLabelProvider<FogBugzFilter> {
    public FilterLabelProvider() {
        super(FogBugzFilter.class);
    }

    @Override
    protected String getTextForElement(FogBugzFilter filter) {
        StringBuilder sb = new StringBuilder();
        sb.append(filter.getDescription());

        if (filter.getType() == FilterType.SHARED) {
            sb.append(" (shared)");
        }
        
        return sb.toString();
    }
}
