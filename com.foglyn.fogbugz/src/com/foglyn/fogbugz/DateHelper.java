package com.foglyn.fogbugz;

import java.util.Calendar;

public class DateHelper {
    public String day(int day, int month, int year) {
        if (day < 1 || day > 31) throw new IllegalArgumentException("Day should be in [1-31] range.");
        if (month < 1 || month > 12) throw new IllegalArgumentException("Month should be in [1-12] range.");
        
        return String.format("%d/%d/%d", month, day, year);
    }
    
    public String day(Calendar cal) {
        return day(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
    }
    
    public String month(int month, int year) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("Month should be in [1-12] range.");

        String name = null;
        switch (month) {
        case 1: name = "January"; break;
        case 2: name = "February"; break;
        case 3: name = "March"; break;
        case 4: name = "April"; break;
        case 5: name = "May"; break;
        case 6: name = "Jun"; break;
        case 7: name = "July"; break;
        case 8: name = "August"; break;
        case 9: name = "September"; break;
        case 10: name = "October"; break;
        case 11: name = "November"; break;
        case 12: name = "December"; break;
        }
        
        return name + " " + year;
    }

    public String month(Calendar cal) {
        return month(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
    }
    
    public String today() {
        return "today";
    }

    public String tomorrow() {
        return "tomorrow";
    }
    
    public String yesterday() {
        return "yesterday";
    }

    public Calendar anotherMonth(int offset) {
        return anotherMonthFrom(Calendar.getInstance(), offset);
    }
    
    Calendar anotherMonthFrom(Calendar from, int weekOffset) {
        Calendar cal = copy(from);
        cal.add(Calendar.MONTH, weekOffset);
        return cal;
    }

    public Calendar anotherWeekStart(int weekOffset) {
        return anotherWeekStartFrom(Calendar.getInstance(), weekOffset);
    }
    
    Calendar anotherWeekStartFrom(Calendar from, int offset) {
        Calendar cal = copy(from);
        cal.add(Calendar.WEEK_OF_YEAR, offset);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        
        return cal;
    }

    public Calendar anotherWeekEnd(int weekOffset) {
        return anotherWeekEndFrom(Calendar.getInstance(), weekOffset);
    }
    
    Calendar anotherWeekEndFrom(Calendar from, int weekOffset) {
        Calendar cal = copy(from);
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset);
        
        int first = cal.getFirstDayOfWeek();
        int last = first - 1;
        if (last < 1) {
            last += 7;
        }
        
        cal.set(Calendar.DAY_OF_WEEK, last);
        
        return cal;
    }
    
    public String anotherDay(int daysOffset) {
        return day(anotherDayFrom(Calendar.getInstance(), daysOffset));
    }
    
    Calendar anotherDayFrom(Calendar from, int daysOffset) {
        Calendar cal = copy(from);
        cal.add(Calendar.DAY_OF_YEAR, daysOffset);
        
        return cal;
    }

    private Calendar copy(Calendar cal) {
        return (Calendar) cal.clone();
    }
    
    public String range(String since, String to) {
        if (since == null && to == null) {
            throw new IllegalArgumentException("At least one argument must be non-null");
        }
        
        if (since == null) return ".." + to;
        if (to == null) return since + "..";
        return since + ".." + to;
    }
}
