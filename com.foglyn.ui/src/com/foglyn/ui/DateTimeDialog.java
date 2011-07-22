package com.foglyn.ui;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class DateTimeDialog extends Dialog {
    private DateTime date;
    private ListViewer time;

    // this is local hourMinute when workday start (computed from FogBugz value, or default)
    private int startOfWorkdayLocalHourMinute;
    
    private String title;
    
    private Date selectedDate;
    
    protected DateTimeDialog(Shell shell, String title, Date selection, int startOfWorkdayLocalHourMinute) {
        super(shell);

        this.title = title;
        
        this.selectedDate = selection;
        this.startOfWorkdayLocalHourMinute = startOfWorkdayLocalHourMinute;
        
        if (startOfWorkdayLocalHourMinute < 0) {
            // default start of workday is 9:00
            this.startOfWorkdayLocalHourMinute = 900;
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite content = new Composite(parent, SWT.NONE);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Label dateLabel = new Label(content, SWT.NONE);
        dateLabel.setText("&Date:");
        
        date = new DateTime(content, SWT.CALENDAR | SWT.BORDER);

        Label timeLabel = new Label(content, SWT.NONE);
        timeLabel.setText("&Time:");
        
        time = new ListViewer(content, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
        time.setContentProvider(new ArrayContentProvider());
        time.setLabelProvider(new TimeLabelProvider());
        time.setInput(getTimeValues());

        Link gotoToday = new Link(content, SWT.NONE);
        gotoToday.setText("<a>&Go to Today</a>");
        gotoToday.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                gotoToday();
            }
        });

        Link workdayStart = new Link(content, SWT.NONE);
        workdayStart.setText("<a>&Start of Workday</a>");
        workdayStart.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                gotoStartOfWorkday();
            }
        });
        
        if (selectedDate == null) {
            gotoToday();
            gotoStartOfWorkday();
            
            recomputeSelectedDate();
        } else {
            gotoSelectedDate();
        }
        
        date.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                recomputeSelectedDate();
            }
        });
        
        time.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                recomputeSelectedDate();
            }
        });

        // Layout stuff
        FormLayout layout = new FormLayout();
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        
        content.setLayout(layout);

        FormData dateLabelData = new FormData();
        dateLabelData.top = new FormAttachment(0);
        dateLabelData.left = new FormAttachment(date, 0, SWT.LEFT);
        
        dateLabel.setLayoutData(dateLabelData);
        
        FormData calData = new FormData();
        calData.top = new FormAttachment(dateLabel, convertVerticalDLUsToPixels(2), SWT.BOTTOM);
        calData.left = new FormAttachment();
//        calData.bottom = new FormAttachment(100);
        
        date.setLayoutData(calData);

        FormData timeLabelData = new FormData();
        timeLabelData.top = new FormAttachment(0);
        timeLabelData.left = new FormAttachment(time.getControl(), 0, SWT.LEFT);
        
        timeLabel.setLayoutData(timeLabelData);
        
        FormData listData = new FormData();
        listData.top = new FormAttachment(date, 0, SWT.TOP);
        listData.bottom = new FormAttachment(date, 0, SWT.BOTTOM);
        listData.left = new FormAttachment(date, convertHorizontalDLUsToPixels(4), SWT.RIGHT);
        listData.right = new FormAttachment(100);

        time.getControl().setLayoutData(listData);

        FormData todayData = new FormData();
        todayData.top = new FormAttachment(date, convertVerticalDLUsToPixels(2), SWT.BOTTOM);
        todayData.left = new FormAttachment(0);
        todayData.bottom = new FormAttachment(100);
        
        gotoToday.setLayoutData(todayData);

        FormData workdayStartData = new FormData();
        workdayStartData.top = new FormAttachment(time.getControl(), convertVerticalDLUsToPixels(2), SWT.BOTTOM);
        workdayStartData.right = new FormAttachment(100);
        workdayStartData.bottom = new FormAttachment(100);
        
        workdayStart.setLayoutData(workdayStartData);
        
        return content;
    }

    private void gotoSelectedDate() {
        Calendar c = Calendar.getInstance();
        
        c.setTime(selectedDate);
        
        date.setYear(c.get(Calendar.YEAR));
        date.setMonth(c.get(Calendar.MONTH));
        date.setDay(c.get(Calendar.DAY_OF_MONTH));

        int hm = c.get(Calendar.HOUR_OF_DAY) * 100 + c.get(Calendar.MINUTE);
        hm = Utils.normalizeToHalfHour(hm);
        
        time.setSelection(new StructuredSelection(Integer.valueOf(hm)), true);
    }

    protected void recomputeSelectedDate() {
        Calendar c = Calendar.getInstance();
        
        c.clear();
        c.set(Calendar.YEAR, date.getYear());
        c.set(Calendar.MONTH, date.getMonth()); // DateTime returns 0 for January
        c.set(Calendar.DAY_OF_MONTH, date.getDay());
        
        int hm = startOfWorkdayLocalHourMinute;
        
        IStructuredSelection selectedTime = (IStructuredSelection) time.getSelection();
        if (selectedTime.getFirstElement() != null) {
            hm = ((Integer) selectedTime.getFirstElement()).intValue();
        }
        
        c.set(Calendar.HOUR, hm / 100);
        c.set(Calendar.MINUTE, hm % 100);
        
        selectedDate = c.getTime();
    }

    void gotoToday() {
        Calendar c = Calendar.getInstance();
        
        // setDate expects 0 as January. Calendar gives us 0 for January.
        
        // TODO: This is better alternative, but available since 3.4 only 
        // date.setDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        
        date.setYear(c.get(Calendar.YEAR));
        date.setMonth(c.get(Calendar.MONTH));
        date.setDay(c.get(Calendar.DAY_OF_MONTH));
        
        Event e = new Event();
        e.item = date;
        
        date.notifyListeners(SWT.Selection, e);
    }
    
    void gotoStartOfWorkday() {
        time.setSelection(new StructuredSelection(Integer.valueOf(this.startOfWorkdayLocalHourMinute)), true);
    }

    private Integer[] getTimeValues() {
        Integer[] timeValues = new Integer[48]; // two values for each hour
        
        int hour = 0;
        int minutes = 0;
        for (int i = 0; i < 48; i++) {
            timeValues[i] = hour * 100 + minutes;
            
            minutes += 30;
            if (minutes == 60) {
                hour ++;
                minutes = 0;
            }
        }
        
        return timeValues;
    }
    
    @Override
    protected boolean isResizable() {
        return false;
    }
    
    private static class TimeLabelProvider extends LabelProvider {
        private DateFormat formatter;
        private Calendar calendar;
        
        TimeLabelProvider() {
            formatter = DateFormat.getTimeInstance(DateFormat.SHORT);

            calendar = Calendar.getInstance();
            calendar.clear();
        }
        
        @Override
        public String getText(Object v) {
            Integer hourMinute = (Integer) v;
            
            int minutes = hourMinute % 100;
            int hours = hourMinute / 100;
            
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            calendar.set(Calendar.MINUTE, minutes);

            return formatter.format(calendar.getTime());
        }
    }
    
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        
        newShell.setText(title);
    }
    
    public Date getSelectedDate() {
        if (selectedDate == null) return null;
        return new Date(selectedDate.getTime());
    }
}
