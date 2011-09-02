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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

public class NullValueLabelProvider extends LabelProvider {
    private final String nullValueLabel;
    private final ILabelProvider delegate;

    public NullValueLabelProvider(String nullValueLabel, ILabelProvider provider) {
        this.nullValueLabel = nullValueLabel;
        this.delegate = provider;
    }
    
    @Override
    public final String getText(Object element) {
        if (element == HelperConstants.NULL_VALUE) {
            return nullValueLabel;
        }

        return delegate.getText(element);
    }
}
