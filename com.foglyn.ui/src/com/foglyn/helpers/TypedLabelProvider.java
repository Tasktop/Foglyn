package com.foglyn.helpers;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public abstract class TypedLabelProvider<T> extends LabelProvider {
    private final Class<T> clazz;

    public TypedLabelProvider(Class<T> clazz) {
        this.clazz = clazz;
    }
    
    @Override
    public final String getText(Object element) {
        T e = clazz.cast(element);
        return getTextForElement(e);
    }

    protected String getTextForElement(T element) {
        return null;
    }
    
    @Override
    public final Image getImage(Object element) {
        T e = clazz.cast(element);
        return getImageForElement(e);
    }
    
    protected Image getImageForElement(T e) {
        return null;
    }
}
