package com.foglyn.ui;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Copied from SingleSelectionAttributeEditor in Mylyn.
 * 
 * Uses standard Combo instead of CCombo.
 */
public class FoglynSingleSelectionAttributeEditor extends AbstractAttributeEditor {

    private String[] values;

    private Combo combo;

    public FoglynSingleSelectionAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
    }

    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        if (isReadOnly()) {
            Text text = new Text(parent, SWT.FLAT | SWT.READ_ONLY);
            text.setFont(JFaceResources.getDefaultFont());
            toolkit.adapt(text, false, false);
            text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
            String label = getValueLabel();
            if ("".equals(label)) { //$NON-NLS-1$
                // if set to the empty string the label will use 64px on GTK 
                text.setText(" "); //$NON-NLS-1$
            } else {
                text.setText(label);
            }
            setControl(text);
        } else {
            combo = new Combo(parent, SWT.FLAT | SWT.READ_ONLY);
            toolkit.adapt(combo, false, false);
            combo.setFont(JFaceResources.getDefaultFont());
            combo.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);

            Map<String, String> labelByValue = getAttributeMapper().getOptions(getTaskAttribute());
            if (labelByValue != null) {
                values = labelByValue.keySet().toArray(new String[0]);
                for (String value : values) {
                    combo.add(labelByValue.get(value));
                }
            }

            combo.setVisibleItemCount(Constants.NUMBER_OF_ENTRIES_IN_COMBOBOX);

            select(getValue(), getValueLabel());

            if (values != null) {
                combo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        int index = combo.getSelectionIndex();
                        if (index > -1) {
                            Assert.isNotNull(values);
                            Assert.isLegal(index >= 0 && index <= values.length - 1);
                            setValue(values[index]);
                        }
                    }
                });
            }

            setControl(combo);
        }
    }

    public String getValue() {
        return getAttributeMapper().getValue(getTaskAttribute());
    }

    public String getValueLabel() {
        return getAttributeMapper().getValueLabel(getTaskAttribute());
    }

    private void select(String value, String label) {
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(value)) {
                    combo.select(i);
                    break;
                }
            }
        } else {
            combo.setText(label);
        }
    }

    public void setValue(String value) {
        getAttributeMapper().setValue(getTaskAttribute(), value);
        attributeChanged();
    }
}
