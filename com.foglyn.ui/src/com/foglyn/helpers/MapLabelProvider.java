package com.foglyn.helpers;

import java.util.Map;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class MapLabelProvider extends LabelProvider {
    private final Map<?, String> strings;
    private final Map<?, Image> images;
    
    public MapLabelProvider(Map<?, String> strings, Map<?, Image> images) {
        this.strings = strings;
        this.images = images;
    }
    
    @Override
    public String getText(Object element) {
        if (strings.containsKey(element)) {
            return strings.get(element);
        }
        
        return super.getText(element);
    }
    
    @Override
    public Image getImage(Object element) {
        if (images.containsKey(element)) {
            return images.get(element);
        }
        
        return super.getImage(element);
    }
}
