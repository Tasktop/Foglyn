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

import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class MappingLabelProvider extends LabelProvider {
    private Map<Object, String> labels;

    public MappingLabelProvider(Map<Object, String> labels) {
        this.labels = labels;
    }
    
    @Override
    public final String getText(Object element) {
        return labels.get(element);
    }

    @Override
    public final Image getImage(Object element) {
        return null;
    }
}
