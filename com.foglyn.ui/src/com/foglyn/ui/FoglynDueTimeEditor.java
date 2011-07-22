package com.foglyn.ui;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.foglyn.core.FoglynConstants;

public class FoglynDueTimeEditor extends AbstractEditorWithHint {
    private DateTimeParser parser;
    
    private Text text;
    
    private int workdayStart;
    private boolean wasDefinedBefore;

    public FoglynDueTimeEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);

        String workdayStartVal = getTaskAttribute().getMetaData().getValue(FoglynConstants.META_WORKDAY_START);
        
        workdayStart = 900; // default workday start is 9:00
        if (workdayStartVal != null) {
            BigDecimal gmtHour = new BigDecimal(workdayStartVal);
            workdayStart = Utils.getLocalHourMinute(gmtHour);
            workdayStart = Utils.normalizeToHalfHour(workdayStart);
        }
        
        String definedBeforeValue = getTaskAttribute().getMetaData().getValue(FoglynConstants.META_NON_EMPTY_PREVIOUS_VALUE);
        wasDefinedBefore = Boolean.parseBoolean(definedBeforeValue);
        
        this.parser = new DateTimeParser(workdayStart);
    }

    @Override
    public void createControl(Composite composite, FormToolkit toolkit) {
        if (isReadOnly()) {
            text = new Text(composite, SWT.FLAT | SWT.READ_ONLY);
            toolkit.adapt(text, false, false);
            text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
            text.setText(getTextValue());
            setControl(text);
            
            return;
        }
        
        Composite c = toolkit.createComposite(composite);

        GridLayoutFactory.fillDefaults().spacing(10, 0).numColumns(2).applyTo(c);
        
        text = new Text(c, SWT.BORDER | SWT.SINGLE);
        toolkit.adapt(text, false, false);

        text.setText(getTextValue());
        text.setToolTipText("Enter date and time. Valid formats for date: 'tomorrow',\n" +
        		"'next <day of week>', 'next week' (meaning next Monday), '+N days',\n" +
        		"'+N weeks', '+N months', " + getUserDateFormat());
        
        decorateTextInput(text);
        
        Date d = getValue();
        if (d == null) {
            showEmptyMessage();
        } else {
            setInfoMessage(getDecorationText(d));
        }

        installListeners(text);
        
        Button b = new Button(c, SWT.PUSH);
        b.setImage(FoglynImages.getImage(FoglynImages.CALENDAR));
        b.setToolTipText("Choose date and time");
        
        b.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                Shell shell = null;
                if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
                    shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                } else {
                    shell = new Shell(PlatformUI.getWorkbench().getDisplay());
                }
                
                DateTimeDialog dtd = new DateTimeDialog(shell, "Select Due Date and Time", getValue(), workdayStart);
                if (dtd.open() == Dialog.OK) {
                    Date d = dtd.getSelectedDate();
                    text.setText(formatDateTime(d));
                    
                    // set date value explicitly and only after we set text
                    // field, because parsing our formatted value sometimes
                    // fails (done in processCurrentValue(), called from text
                    // field)
                    setValue(d);
                    setInfoMessage(getDecorationText(d));
                }
            }
        });
        
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(text);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(b);

        setControl(c);
    }

    void showEmptyMessage() {
        if (wasDefinedBefore) {
            setErrorMessage("Due time cannot be cleared and will remain unchanged");
        } else {
            setInfoMessage("No due time");
        }
    }
    
    @Override
    void processCurrentValue() {
        String t = text.getText();

        if (t == null || t.trim().length() == 0) {
            setValue(null);
            showEmptyMessage();
            return;
        }
        
        try {
            Date date = parser.parse(t);
            
            setValue(date);
            setInfoMessage(getDecorationText(date));
        } catch (IllegalArgumentException e) {
            setValue(null);
            
            // failed to parse
            setErrorMessage(e.getMessage());
        }
    }
    
    private String getTextValue() {
        Date date = getValue();
        if (date != null) {
            return formatDateTime(date);
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    public Date getValue() {
        return getAttributeMapper().getDateValue(getTaskAttribute());
    }

    public void setValue(Date date) {
        getAttributeMapper().setDateValue(getTaskAttribute(), date);
        attributeChanged();
    }

    static String formatDateTime(Date date) {
        return MessageFormat.format("{0,date,medium} {0,time,short}", date);
    }
    
    static String getDecorationText(Date date) {
        // always use English day of week, as rest of interface is English
        
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date);
        return MessageFormat.format("{0,date,medium}, {0,time,short} ({1})", date, dayOfWeek);
    }

    static String getUserDateFormat() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        if (df instanceof SimpleDateFormat) {
            SimpleDateFormat sdf = (SimpleDateFormat) df;
            return "'" + sdf.toPattern().toUpperCase() + "'";
        }
        
        return "local date format";
    }
    
    static DateFormat getDateTimeFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    }
    
    @Override
    protected void decorateIncoming(Color color) {
        if (text != null) {
            text.setBackground(color);
        }
    }
}
