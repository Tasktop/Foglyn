package com.foglyn.ui;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses date/time written in various formats.
 */
class DateTimeParser {
    private static final int DTP_DAY = 0;
    private static final int DTP_MONTH = 1; // 0-based
    private static final int DTP_YEAR = 2;
    private static final int DTP_HOUR = 3;
    private static final int DTP_MINUTE = 4;
    
    private final int workdayStartLocalHourMinute;
    
    private final Pattern amPmPattern = Pattern.compile("(am|a|pm|p)", Pattern.CASE_INSENSITIVE);
    private final Pattern hourColonPattern = Pattern.compile("([0-9]{1,2}+):([012345]?[0123456789])");
    
    private final Pattern todayPattern = Pattern.compile("today|toda|tod|to|t", Pattern.CASE_INSENSITIVE);
    private final Pattern tomorrowPattern = Pattern.compile("tomorrow|tomorro|tomorr|tomor|tomo|tom", Pattern.CASE_INSENSITIVE);
    private final Pattern plusDaysPattern = Pattern.compile("(\\+|in\\s)\\s*([0-9]+)\\s*(days|day|da|d)?", Pattern.CASE_INSENSITIVE);
    private final Pattern inWeeksPattern = Pattern.compile("(\\+|in\\s)\\s*([0-9]+)\\s*(weeks|week|wee|we|w)", Pattern.CASE_INSENSITIVE);
    private final Pattern inMonthsPattern = Pattern.compile("(\\+|in\\s)\\s*([0-9]+)\\s*(months|month|mon|mo|m)", Pattern.CASE_INSENSITIVE);
    private final Pattern nextDayInWeekPattern = Pattern.compile("(next\\s+)?(" +
    		"monday|monda|mond|mon|mo|m|" +
    		"tuesday|tuesda|tuesd|tues|tue|tu|" + // "t" would conflict with thursday
    		"wednesday|wednesda|wednesd|wednes|wedne|wedn|wed|we|w|" +
    		"thursday|thursda|thursd|thurs|thur|thu|th|" +
    		"friday|frida|frid|fri|fr|f|" +
    		"saturday|saturda|saturd|satur|satu|sat|sa|" +
    		"sunday|sunda|sund|sun|su" +
    		")", Pattern.CASE_INSENSITIVE);

    private final Pattern nextWeek = Pattern.compile("next\\s+(week|wee|we|w)", Pattern.CASE_INSENSITIVE);
    private final Pattern nextMonth = Pattern.compile("next\\s+(month|mont|mon|mo|m)", Pattern.CASE_INSENSITIVE);
    
    private final DateFormat shortDateFormat;
    private final DateFormat mediumDateFormat;
    
    DateTimeParser(int workdayStartLocalHourMinute) {
        this(workdayStartLocalHourMinute, Locale.getDefault());
    }

    DateTimeParser(int workdayStartLocalHourMinute, Locale locale) {
        this.workdayStartLocalHourMinute = workdayStartLocalHourMinute;
        
        shortDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        shortDateFormat.setLenient(false);
        
        mediumDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        mediumDateFormat.setLenient(false);
    }
    
    Date parse(String input) {
        int[] result = new int[] {-1, -1, -1, -1, -1};

        List<String> tokenizedInput = new ArrayList<String>(Arrays.asList(input.split("\\s+|,\\s+|,")));
        if (tokenizedInput.isEmpty()) throw new IllegalArgumentException("Cannot parse date/time: " + input);
        
        parseTime(input,tokenizedInput, result);

        parseDate(tokenizedInput, result);

        return convert(result);
    }

    private void parseDate(List<String> tokenizedInput, int[] result) {
        String rest = join(tokenizedInput);

        Matcher m = todayPattern.matcher(rest);
        if (m.matches()) {
            todayPlusDays(0, result);
            return;
        }
        
        m.usePattern(tomorrowPattern);
        if (m.matches()) {
            todayPlusDays(1, result);
            return;
        }
        
        m.usePattern(plusDaysPattern);
        if (m.matches()) {
            int days = Integer.valueOf(m.group(2));
            todayPlusDays(days, result);
            return;
        }

        m.usePattern(inWeeksPattern);
        if (m.matches()) {
            int weeks = Integer.valueOf(m.group(2));
            todayPlusDays(weeks * 7, result);
            return;
        }

        m.usePattern(inMonthsPattern);
        if (m.matches()) {
            int months = Integer.valueOf(m.group(2));
            todayPlusMonths(months, result);
            return;
        }

        m.usePattern(nextDayInWeekPattern);
        if (m.matches()) {
            String day = m.group(2).toLowerCase();
            
            int dayInWeek = -1;
            if (day.startsWith("m")) dayInWeek = Calendar.MONDAY;
            if (day.startsWith("tu")) dayInWeek = Calendar.TUESDAY;
            if (day.startsWith("w")) dayInWeek = Calendar.WEDNESDAY;
            if (day.startsWith("th")) dayInWeek = Calendar.THURSDAY;
            if (day.startsWith("f")) dayInWeek = Calendar.FRIDAY;
            if (day.startsWith("sa")) dayInWeek = Calendar.SATURDAY;
            if (day.startsWith("su")) dayInWeek = Calendar.SUNDAY;
            
            if (dayInWeek < 0) {
                // should not happen
                throw new IllegalArgumentException("Cannot parse day of week: " + day);
            }
            
            nextDayOfWeek(dayInWeek,result);
            return;
        }
        
        m.usePattern(nextWeek);
        if (m.matches()) {
            nextWeek(result);
            return;
        }

        m.usePattern(nextMonth);
        if (m.matches()) {
            nextMonth(result);
            return;
        }

        if (parseDateFromFormat(shortDateFormat, rest, result)) {
            return;
        }

        if (parseDateFromFormat(mediumDateFormat, rest, result)) {
            return;
        }
        
        throw new IllegalArgumentException("Cannot parse date: " + rest);
    }

    private boolean parseDateFromFormat(DateFormat dateFormat, String rest, int[] result) {
        try {
            setDate(dateFormat.parse(rest), result);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private String join(List<String> tokenizedInput) {
        StringBuilder sb = new StringBuilder();
        
        String delim = "";
        for (String s: tokenizedInput) {
            sb.append(delim);
            delim = " ";
            sb.append(s);
        }
        
        return sb.toString();
    }

    private void parseTime(String input, List<String> tokenizedInput, int[] result) {
        Matcher m = amPmPattern.matcher(tokenizedInput.get(tokenizedInput.size() - 1));
        
        String ampm = null;
        if (m.matches()) {
            ampm = m.group(0);
            tokenizedInput.remove(tokenizedInput.size() - 1);
            if (tokenizedInput.isEmpty()) throw new IllegalArgumentException("Cannot parse date/time: " + input);
        }
        
        m = hourColonPattern.matcher(tokenizedInput.get(tokenizedInput.size() - 1));
        if (m.matches()) {
            int hour = Integer.valueOf(m.group(1));
            int min = Integer.valueOf(m.group(2));
            
            if (ampm != null && hour > 12) {
                throw new IllegalArgumentException("Unknown time: " + tokenizedInput.get(tokenizedInput.size() - 1) + " " + ampm);
            }

            if (ampm != null) {
                boolean am = ampm.charAt(0) == 'a' || ampm.charAt(0) == 'A';
                if (hour == 12 && am) {
                    hour = 0;
                }
                if (hour != 12 && !am) {
                    hour += 12;
                }
            }
            result[DTP_HOUR] = hour;
            result[DTP_MINUTE] = min;

            tokenizedInput.remove(tokenizedInput.size() - 1);
            return;
        }

        startOfWorkday(result);
    }

    private Date convert(int[] result) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, result[DTP_YEAR]);
        c.set(Calendar.MONTH, result[DTP_MONTH]);
        c.set(Calendar.DAY_OF_MONTH, result[DTP_DAY]);
        c.set(Calendar.HOUR_OF_DAY, result[DTP_HOUR]);
        c.set(Calendar.MINUTE, result[DTP_MINUTE]);
        
        return c.getTime();
    }

    private void startOfWorkday(int[] result) {
        result[DTP_HOUR] = workdayStartLocalHourMinute / 100;
        result[DTP_MINUTE] = workdayStartLocalHourMinute % 100;
    }
    
    private void todayPlusDays(int plusDays, int[] result) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, plusDays);
        
        result[DTP_YEAR] = c.get(Calendar.YEAR);
        result[DTP_MONTH] = c.get(Calendar.MONTH);
        result[DTP_DAY] = c.get(Calendar.DAY_OF_MONTH);
    }

    private void todayPlusMonths(int months, int[] result) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, months);
        
        result[DTP_YEAR] = c.get(Calendar.YEAR);
        result[DTP_MONTH] = c.get(Calendar.MONTH);
        result[DTP_DAY] = c.get(Calendar.DAY_OF_MONTH);
    }

    private void nextDayOfWeek(int dayOfWeek, int[] result) {
        Calendar c = Calendar.getInstance();
        int d = c.get(Calendar.DAY_OF_WEEK);
        
        int add = dayOfWeek - d;
        if (add <= 0) add += 7;
        c.add(Calendar.DAY_OF_WEEK, add);
        
        result[DTP_YEAR] = c.get(Calendar.YEAR);
        result[DTP_MONTH] = c.get(Calendar.MONTH);
        result[DTP_DAY] = c.get(Calendar.DAY_OF_MONTH);
    }

    private void nextWeek(int[] result) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        c.add(Calendar.WEEK_OF_MONTH, 1);
        
        result[DTP_YEAR] = c.get(Calendar.YEAR);
        result[DTP_MONTH] = c.get(Calendar.MONTH);
        result[DTP_DAY] = c.get(Calendar.DAY_OF_MONTH);
    }

    private void nextMonth(int[] result) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, 1);
        
        result[DTP_YEAR] = c.get(Calendar.YEAR);
        result[DTP_MONTH] = c.get(Calendar.MONTH);
        result[DTP_DAY] = c.get(Calendar.DAY_OF_MONTH);
    }

    private void setDate(Date date, int[] result) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        
        result[DTP_YEAR] = c.get(Calendar.YEAR);
        result[DTP_MONTH] = c.get(Calendar.MONTH);
        result[DTP_DAY] = c.get(Calendar.DAY_OF_MONTH);
    }
}
