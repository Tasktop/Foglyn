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
