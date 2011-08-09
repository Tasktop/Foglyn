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
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.foglyn.fogbugz.DaysHoursMinutes;

public class Utils {
    static String formatDaysHoursMinutes(DaysHoursMinutes dhm) {
        String days = MessageFormat.format("{0,choice,0#|1#{0} day|2#{0} days}", dhm.days);
        String hours = MessageFormat.format("{0,choice,0#|1#{0} hour|2#{0} hours}", dhm.hours);
        String minutes = MessageFormat.format("{0,choice,0#|1#{0} minute|2#{0} minutes}", dhm.minutes);
        
        if (days.length() == 0 && hours.length() == 0 && minutes.length() == 0) {
            return "0 hours";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(days);
        if (hours.length() > 0 && sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(hours);
        
        if (minutes.length() > 0 && sb.length() > 0) {
            sb.append(" ");
        }
        sb.append(minutes);
        
        return sb.toString();
    }

    public static String formatHoursMinutes(DaysHoursMinutes dhm) {
        return MessageFormat.format("{0,choice,1#{0} hour|2#{0} hours} and {1,choice,0#{1} minutes|1#{1} minute|2#{1} minutes}", dhm.hours, dhm.minutes);
    }

    static int getGMTHour(int localHour) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, localHour);
        
        Date t = c.getTime();
        
        c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.setTime(t);
    
        return c.get(Calendar.HOUR_OF_DAY);
    }

    static int getLocalHourMinute(BigDecimal gmtHour) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
    
        int hour = gmtHour.intValue();
    
        c.set(Calendar.HOUR_OF_DAY, hour);
        Date t = c.getTime();
        
        c = Calendar.getInstance();
        c.setTime(t);
        
        int localHour = c.get(Calendar.HOUR_OF_DAY);
        
        int minuteV = gmtHour.subtract(gmtHour.setScale(0, RoundingMode.FLOOR)).multiply(new BigDecimal("60")).intValue();
        
        return localHour * 100 + minuteV;
    }

    static int normalizeToHalfHour(int hourMinute) {
        int h = hourMinute / 100;
        int m = hourMinute % 100;
        
        if (m < 30) {
            m = 0;
        } else {
            m = 30;
        }
        
        hourMinute = h * 100 + m;
        return hourMinute;
    }
}
