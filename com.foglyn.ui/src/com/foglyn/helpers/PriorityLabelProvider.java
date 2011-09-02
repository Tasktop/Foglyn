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

import com.foglyn.fogbugz.FogBugzPriority;

public class PriorityLabelProvider extends TypedLabelProvider<FogBugzPriority> {
    public PriorityLabelProvider() {
        super(FogBugzPriority.class);
    }

    @Override
    protected String getTextForElement(FogBugzPriority ff) {
        return ff.toUserString();
    }
}
