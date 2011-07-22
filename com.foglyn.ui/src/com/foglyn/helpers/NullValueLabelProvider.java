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
