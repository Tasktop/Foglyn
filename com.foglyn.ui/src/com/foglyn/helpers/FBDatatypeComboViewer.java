package com.foglyn.helpers;

import java.util.Collection;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;

import com.foglyn.fogbugz.HasID;
import com.foglyn.fogbugz.ID;

public class FBDatatypeComboViewer<I extends ID, T extends HasID<I>> extends ComboViewer {

    public static <I extends ID, T extends HasID<I>> FBDatatypeComboViewer<I, T> create(Class<T> clazz, Composite parent, int style, String nullValueLabel, ILabelProvider labelProvider) {
        FBDatatypeComboViewer<I, T> combo = new FBDatatypeComboViewer<I, T>(clazz, parent, style);
        
        combo.setContentProvider(new CollectionContentProvider(true));
        combo.setLabelProvider(new NullValueLabelProvider(nullValueLabel, labelProvider));
        
        return combo;
    }
    
    private final Class<T> clazz;
    
    public FBDatatypeComboViewer(Class<T> clazz, Composite parent, int style) {
        super(parent, style);
        this.clazz = clazz;
        
    }

    public I getSelectedID() {
        return convertSelection(getSelection());
    }

    public I convertSelection(ISelection selection) {
        IStructuredSelection sel = (IStructuredSelection) selection;
        
        Object obj = sel.getFirstElement();
        if (obj == null) {
            return null;
        }
        
        if (obj.equals(HelperConstants.NULL_VALUE)) {
            return null;
        }
        
        T castedObj = clazz.cast(obj);
        return castedObj.getID();
    }
    
    public void setComboValuesFromClientValues(Collection<T> values) {
        I sel = getSelectedID();
        
        setInput(values);
        
        for (T v: values) {
            if (v.getID().equals(sel)) {
                setSelection(new StructuredSelection(v), true);
                return;
            }
        }
        
        setSelection(HelperConstants.NULL_VALUE_SELECTION, true);
    }

    @SuppressWarnings("unchecked")
    public void selectOption(I value) {
        if (value == null) {
            setSelection(HelperConstants.NULL_VALUE_SELECTION, true);
            return;
        }

        // this is potentionally bad ... 
        Collection<T> colls = (Collection<T>) getInput();
        
        for (T o: colls) {
            if (value.equals(o.getID())) {
                setSelection(new StructuredSelection(o), true);
                return;
            }
        }
    }
}
