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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

public class HelperConstants {
    /**
     * Value supplied as "null" value in content provider.
     */
    public static final Object NULL_VALUE = new Object();
    
    /**
     * Selection with NULL_VALUE.
     */
    public static final IStructuredSelection NULL_VALUE_SELECTION = new StructuredSelection(NULL_VALUE);
}
