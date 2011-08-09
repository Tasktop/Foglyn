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

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;

import com.foglyn.core.AdvancedSearchQuery.DateCondition;

public class DateConditionComboViewer extends ComboViewer {
    public static DateConditionComboViewer create(Composite parent, int style, String nullValueLabel) {
        DateConditionComboViewer combo = new DateConditionComboViewer(parent, style);
        
        combo.setContentProvider(new CollectionContentProvider(true));
        combo.setLabelProvider(new NullValueLabelProvider(nullValueLabel, new DateConditionProvider()));
        
        return combo;
    }
    
    public DateConditionComboViewer(Composite parent, int style) {
        super(parent, style);
    }

    public void setDateConditionsInput(List<DateCondition> input) {
        ISelection sel = getSelection();
        
        setInput(input);
        setSelection(sel, true);
    }

    public DateCondition getDateCondition() {
        IStructuredSelection sel = (IStructuredSelection) getSelection();
        
        Object obj = sel.getFirstElement();
        if (obj.equals(HelperConstants.NULL_VALUE)) {
            return null;
        }
        
        return (DateCondition) obj;
    }
    
    public void selectDateCondition(DateCondition condition) {
        if (condition == null) {
            setSelection(HelperConstants.NULL_VALUE_SELECTION, true);
            return;
        }
        
        Collection<?> colls = (Collection<?>) getInput();
        
        for (Object o: colls) {
            if (condition.equals(o)) {
                setSelection(new StructuredSelection(o), true);
                return;
            }
        }
    }
}
