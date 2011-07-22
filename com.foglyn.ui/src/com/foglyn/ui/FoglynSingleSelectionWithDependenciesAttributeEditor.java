package com.foglyn.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.foglyn.core.FoglynConstants;
import com.foglyn.core.FoglynConstants.Dependency;

public class FoglynSingleSelectionWithDependenciesAttributeEditor extends AbstractAttributeEditor {
    private static class Option {
        public final String value;
        public final String label;
        public final boolean isDefault;
        
        Option(String value, String label, boolean isDefault) {
            this.value = value;
            this.label = label;
            this.isDefault = isDefault;
        }
        
        @Override
        public String toString() {
            return "Label: " + label + ", Value: " + value;
        }
    }
    
	private Combo combo;
	
	private Map<Dependency, String> dependancies = new EnumMap<Dependency, String>(Dependency.class);
    private List<Option> options;
	
	public FoglynSingleSelectionWithDependenciesAttributeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
		super(manager, taskAttribute);
	}

	void setDependencies(Map<Dependency, String> depValues) {
	    this.dependancies.clear();
	    
	    for (Entry<Dependency, String> e: depValues.entrySet()) {
	        this.dependancies.put(e.getKey(), e.getValue());
	    }
	}

    public void setMasterValue(Dependency meta, String value) {
        if (this.dependancies.containsKey(meta)) {
            this.dependancies.put(meta, value);
            setValues();
        }
    }
	
	@Override
	public void setReadOnly(boolean readOnly) {
	    if (readOnly) {
	        throw new IllegalArgumentException("FoglynSingleSelectionAttributeEditor doesn't support read-only mode");
	    }
	    
	    super.setReadOnly(readOnly);
	}
	
	@Override
	public void createControl(Composite parent, FormToolkit toolkit) {
		combo = new Combo(parent, SWT.FLAT | SWT.READ_ONLY);
		toolkit.adapt(combo, false, false);
		combo.setFont(JFaceResources.getDefaultFont());
		combo.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
        combo.setVisibleItemCount(Constants.NUMBER_OF_ENTRIES_IN_COMBOBOX);

		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int index = combo.getSelectionIndex();
				if (index > -1) {
					Assert.isNotNull(options);
					setSelectedOption(options.get(index));
				}
			}
		});

		setValues();
		
		setControl(combo);
	}

	private void setValues() {
	    filterOptions();
        select();
	}

    private void filterOptions() {
        options = new ArrayList<Option>();
        
        Map<String, TaskAttribute> valueAttributes = getTaskAttribute().getAttributes();
        for (TaskAttribute a: valueAttributes.values()) {
            TaskAttributeMetaData m = a.getMetaData();
            
            boolean add = true;
            for (Entry<Dependency, String> de: dependancies.entrySet()) {
                String metaValue = m.getValue(de.getKey().getKey());
                if (metaValue != null && !metaValue.equals(de.getValue())) {
                    // ignore this value
                    add = false;
                }
            }

            String value = a.getMetaData().getValue(FoglynConstants.META_VALUE_ID);
            if (value == null) {
                value = a.getId();
            }
            
            boolean isDefault = Boolean.parseBoolean(a.getMetaData().getValue(FoglynConstants.META_DEFAULT_VALUE));
            
            if (add) {
                options.add(new Option(value, a.getValue(), isDefault));
            }
        }
    }
	
	/**
	 * Selects currently selected option in combo box, or default option if current option cannot be selected.
	 */
    private void select() {
	    // setup combo box
	    combo.removeAll();
        
        for (Option o: options) {
            combo.add(o.label);
        }
        
        if (!selectInCombo(getSelectedOption()) && !options.isEmpty()) {
            if (getSetDefaultValue()) {
                Option defaultOpt = null;
                for (Option o: options) {
                    if (defaultOpt == null && o.isDefault) {
                        defaultOpt = o;
                    }
                }
    
                if (defaultOpt == null) {
                    defaultOpt = options.get(0); // first value will be default
                }
                
                setSelectedOption(defaultOpt);
                selectInCombo(defaultOpt);
            }
        }
    }

    private boolean getSetDefaultValue() {
        boolean setDefault = true;
        String value = getTaskAttribute().getMetaData().getValue(FoglynConstants.META_SET_DEFAULT);
        if (value != null) {
            setDefault = Boolean.parseBoolean(value);
        }
        
        return setDefault;
    }
	
	private boolean selectInCombo(Option opt) {
	    if (opt == null) {
	        combo.deselectAll();
	        return false;
	    }
	    
        for (int i = 0; i < options.size(); i++) {
            Option o = options.get(i);
            if (o.value.equals(opt.value)) {
                combo.select(i);
                return true;
            }
        }
		
		return false;
	}

    private Option getSelectedOption() {
        String value = getTaskAttribute().getValue();
        for (Option o: options) {
            if (o.value.equals(value)) {
                return o;
            }
        }
        
        return null;
    }
    
	private void setSelectedOption(Option opt) {
		getAttributeMapper().setValue(getTaskAttribute(), opt.value);
		attributeChanged();
	}

    boolean isDisposed() {
        return combo != null && combo.isDisposed();
    }
}
