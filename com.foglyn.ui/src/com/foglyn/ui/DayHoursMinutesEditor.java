package com.foglyn.ui;

import java.math.BigDecimal;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.foglyn.core.FoglynConstants;
import com.foglyn.fogbugz.DaysHoursMinutes;

public class DayHoursMinutesEditor extends AbstractEditorWithHint {
    private DaysHoursMinutesParser parser;
    
    private BigDecimal workingHoursPerDay;
    private DaysHoursMinutes workingHoursPerDayDHM;

    private Text text;

    private boolean wasDefinedBefore;
    
    public DayHoursMinutesEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
        
        parser = new DaysHoursMinutesParser();

        String wh = taskAttribute.getMetaData().getValue(FoglynConstants.META_WORKING_HOURS_PER_DAY);
        if (wh != null) {
            this.workingHoursPerDay = new BigDecimal(wh);
            this.workingHoursPerDayDHM = DaysHoursMinutes.fromHours(workingHoursPerDay);
        }
        
        String definedBeforeValue = getTaskAttribute().getMetaData().getValue(FoglynConstants.META_NON_EMPTY_PREVIOUS_VALUE);
        wasDefinedBefore = Boolean.parseBoolean(definedBeforeValue);
    }

    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        if (isReadOnly()) {
            Text field = new Text(parent, SWT.SINGLE | SWT.FLAT | SWT.READ_ONLY);
            toolkit.adapt(field, false, false);

            field.setText(formatDaysHoursMinutes(getValue()));
            setControl(field);
            return;
        }

        text = new Text(parent, SWT.SINGLE | SWT.BORDER);
        text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
        
        toolkit.adapt(text, false, false);
        decorateTextInput(text);
        
        text.setToolTipText("Use 'x days, x hours, x minutes' format.");
        
        DaysHoursMinutes v = getValue();
        text.setText(formatDaysHoursMinutes(v));

        if (v == null) {
            showEmptyMessage();
        } else {
            setInfoMessage(formatDaysHoursMinutes(v));
        }
        
        installListeners(text);
        
        setControl(text);
    }
    
    DaysHoursMinutes getValue() {
        String value = getTaskAttribute().getValue();
        if (value == null) return null;
        value = value.trim();
        if (value.length() == 0) return null;
        
        return DaysHoursMinutes.parseDaysHoursMinutesSlashForm(value);
    }
    
    private static String formatDaysHoursMinutes(DaysHoursMinutes value) {
        if (value == null) return "";
        return Utils.formatDaysHoursMinutes(value);
    }
    
    protected void setValue(DaysHoursMinutes v) {
        if (v == null) {
            getAttributeMapper().setValue(getTaskAttribute(), "");
        } else {
            getAttributeMapper().setValue(getTaskAttribute(), v.toString());
        }
        
        attributeChanged();
    }

    void showEmptyMessage() {
        if (wasDefinedBefore) {
            setErrorMessage("Estimate cannot be cleared and will remain unchanged");
        } else {
            setInfoMessage("No estimate");
        }
    }
    
    @Override
    void processCurrentValue() {
        String value = text.getText();
        if (value.trim().length() == 0) {
            setValue(null);
            
            showEmptyMessage();
            return;
        }
        
        try {
            DaysHoursMinutes v = parser.parse(value);
            
            boolean daysChanged = false;
            
            if (workingHoursPerDay != null) {
                DaysHoursMinutes nv = v.normalize(workingHoursPerDay);
                
                if (v.days.compareTo(nv.days) != 0) {
                    daysChanged = true;
                }
                
                v = nv;
            }
                
            setValue(v);

            String desc = Utils.formatDaysHoursMinutes(v);
            if (daysChanged) {
                desc = desc + "\n(using " + Utils.formatHoursMinutes(workingHoursPerDayDHM) + " per day)";
            }
            
            setInfoMessage(desc);
        } catch (IllegalArgumentException ex) {
            setErrorMessage(ex.getMessage());
        } catch (ArithmeticException ex) {
            setErrorMessage("Cannot compute result value");
        }
    }

    @Override
    protected void decorateIncoming(Color color) {
        if (text != null) {
            text.setBackground(color);
        }
    }
}
