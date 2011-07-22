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
