/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

package com.foglyn.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.foglyn.fogbugz.DaysHoursMinutes;

class DaysHoursMinutesParser {
    private static final String whiteSpace = "\\s*+";
    private static final String days = "(days|day|da|d)";
    private static final String hours = "(hours|hour|hou|ho|h)";
    private static final String minutes = "(minutes|minute|minut|minu|min|mi|m)";
    private static final String unit = "(" + days + "|" + hours + "|" + minutes + ")";
        
    private String decimalSeparator = "\\.";

    private Pattern decimalPattern;
    
    private Pattern unitPattern;
    private Pattern daysPattern;
    private Pattern hoursPattern;
    private Pattern minutesPattern;
    
    private Pattern hourColonPattern;

    DaysHoursMinutesParser() {
        buildPatterns();
    }
    
    private void buildPatterns() {
        String digit = "([0-9])";

        String numeral = "("+digit+"++)";
        String decimal = "("+numeral+"|"+numeral + decimalSeparator + digit + "*+|"+ decimalSeparator + digit + "++)";

        decimalPattern = Pattern.compile(decimal);

        unitPattern = Pattern.compile(unit);
        
        daysPattern = Pattern.compile(decimal + whiteSpace + days);
        hoursPattern = Pattern.compile(decimal + whiteSpace + hours);
        minutesPattern = Pattern.compile(decimal + whiteSpace + minutes);
        
        hourColonPattern = Pattern.compile("(" + digit + "{1,2}+):([012345]?[0123456789])"); 
    }
    
    DaysHoursMinutes parse(String value) {
        List<String> list = splitTokens(value);

        List<String> numerals = new ArrayList<String>();
        
        BigDecimal days = null;
        BigDecimal hours = null;
        BigDecimal minutes = null;
        
        for (String t: list) {
            Matcher m = daysPattern.matcher(t);
            if (m.matches()) {
                days = setIfNull(days, m.group(1), "Days", t);
                
                continue;
            }
            
            m.usePattern(hoursPattern);
            if (m.matches()) {
                hours = setIfNull(hours, m.group(1), "Hours", t);
                
                continue;
            }

            m.usePattern(minutesPattern);
            if (m.matches()) {
                minutes = setIfNull(minutes, m.group(1), "Minutes", t);
                
                continue;
            }
            
            m.usePattern(hourColonPattern);
            if (m.matches()) {
                hours = setIfNull(hours, m.group(1), "Hours", t);
                minutes = setIfNull(minutes, m.group(3), "Minutes", t);
                
                continue;
            }
            
            if (isDecimal(t)) {
                numerals.add(t);
            } else {
                throw new IllegalArgumentException("Cannot parse value: " + t);
            }
        }
        
        if (numerals.size() > 3) {
            throw new IllegalArgumentException("Too many numbers: " + numerals);
        }
        
        if (numerals.size() == 3) {
            if (countNulls(days, hours, minutes) < 3) {
                throw new IllegalArgumentException("More numbers not expected: " + numerals);
            }
            
            days = setIfNull(days, numerals.get(0), "Days", numerals.get(0));
            hours = setIfNull(hours, numerals.get(1), "Hours", numerals.get(1));
            minutes = setIfNull(minutes, numerals.get(2), "Minutes", numerals.get(2));
        }
        
        if (numerals.size() == 2) {
            if (countNulls(days, hours, minutes) < 2) {
                throw new IllegalArgumentException("More numbers not expected: " + numerals);
            }
            
            if (hours == null && minutes == null) {
                hours = new BigDecimal(numerals.get(0));
                minutes = new BigDecimal(numerals.get(1));
            } else if (days == null && minutes == null) {
                days = new BigDecimal(numerals.get(0));
                minutes = new BigDecimal(numerals.get(1));
            } else if (days == null && hours == null) {
                days = new BigDecimal(numerals.get(0));
                hours = new BigDecimal(numerals.get(1));
            }
        }
        
        if (numerals.size() == 1) {
            if (countNulls(days, hours, minutes) < 1) {
                throw new IllegalArgumentException("More numbers not expected: " + numerals);
            }
            
            if (hours == null) {
                hours = new BigDecimal(numerals.get(0));
            } else if (minutes == null) {
                minutes = new BigDecimal(numerals.get(0));
            } else if (days == null) {
                days = new BigDecimal(numerals.get(0));
            }
        }
        
        if (days == null) days = BigDecimal.ZERO;
        if (hours == null) hours = BigDecimal.ZERO;
        if (minutes == null) minutes = BigDecimal.ZERO;
        
        return DaysHoursMinutes.daysHoursMinutes(days, hours, minutes);
    }

    private int countNulls(BigDecimal... decimals) {
        int count = 0;
        for (BigDecimal d: decimals) {
            if (d == null) count ++;
        }
        return count;
    }

    private BigDecimal setIfNull(BigDecimal orig, String value, String desc, String token) {
        if (orig != null) {
            throw new IllegalArgumentException(desc + " not expected: " + token);
        }
        
        return new BigDecimal(value);
    }
    
    private List<String> splitTokens(String value) {
        String[] subs = value.split("\\s+|,\\s+|,");
        
        List<String> list = Arrays.asList(subs);
        list = joinUnits(list);
        return list;
    }

    private List<String> joinUnits(List<String> list) {
        List<String> result = new ArrayList<String>();
        
        boolean wasDecimal = false;
        for (int i = 0; i < list.size(); i++) {
            String val = list.get(i);
            
            if (val.trim().length() == 0) {
                continue;
            }
            
            if (isDecimal(val)) {
                wasDecimal = true;
                result.add(val);
            } else if (isUnit(val)) {
                if (wasDecimal) {
                    String joined = result.get(result.size() - 1) + " " + val;
                    result.set(result.size() - 1, joined);
                }
                wasDecimal = false;
            } else {
                result.add(val);
            }
        }
        
        return result;
    }
    
    boolean isDecimal(String val) {
        Matcher m = decimalPattern.matcher(val);
        return m.matches();
    }

    boolean isUnit(String val) {
        Matcher m = unitPattern.matcher(val);
        return m.matches();
    }
}
