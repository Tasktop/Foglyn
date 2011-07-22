package com.foglyn.fogbugz;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class DaysHoursMinutes {
    private final static BigDecimal SIXTY = BigDecimal.valueOf(60);
    
    public static final DaysHoursMinutes ZERO = daysHoursMinutes(0, 0, 0);
    
    public final BigDecimal days;
    public final BigDecimal hours;
    public final BigDecimal minutes;

    // Parses "days/hours/minutes" form
    public static DaysHoursMinutes parseDaysHoursMinutesSlashForm(String dhmSlashForm) {
        String[] subs = dhmSlashForm.split("/");
        if (subs.length != 3) {
            throw new IllegalArgumentException("Illegal format: " + dhmSlashForm);
        }
        
        try {
            BigDecimal days = new BigDecimal(subs[0]);
            BigDecimal hours = new BigDecimal(subs[1]);
            BigDecimal minutes = new BigDecimal(subs[2]);
            
            return new DaysHoursMinutes(days, hours, minutes);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal format: " + dhmSlashForm);
        }
    }

    /**
     * Converts hours value to hours, minutes. Returned Days=0, hours = (int) orig. hours and minutes = remaining part from orig. hours.
     * 
     * @param hours
     * @return
     */
    public static DaysHoursMinutes fromHours(BigDecimal hours) {
        BigDecimal h = hours.setScale(0, RoundingMode.DOWN);
        BigDecimal m = hours.subtract(h).multiply(SIXTY).setScale(0, RoundingMode.HALF_EVEN);
        
        return daysHoursMinutes(BigDecimal.ZERO, h, m);
    }
    
    public static DaysHoursMinutes daysHoursMinutes(int days, int hours, int minutes) {
        return new DaysHoursMinutes(new BigDecimal(days), new BigDecimal(hours), new BigDecimal(minutes));
    }

    public static DaysHoursMinutes daysHoursMinutes(BigDecimal days, BigDecimal hours, BigDecimal minutes) {
        return new DaysHoursMinutes(days, hours, minutes);
    }
    
    DaysHoursMinutes(BigDecimal days, BigDecimal hours, BigDecimal minutes) {
        if (days == null || hours == null || minutes == null) throw new NullPointerException();
        
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
    }

    @Override
    public int hashCode() {
        int result = 31 + days.hashCode();
        result = 31 * result + hours.hashCode();
        result = 31 * result + minutes.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DaysHoursMinutes)) return false;

        DaysHoursMinutes other = (DaysHoursMinutes) obj;
        
        if (days.compareTo(other.days) != 0) return false;
        if (hours.compareTo(other.hours) != 0) return false;
        if (minutes.compareTo(other.minutes) != 0) return false;
        
        return true;
    }

    /**
     * @param workingHoursPerDay
     * @return normalized value, according to working hours per day
     * @throws ArithmeticException in case of arithmetic problems
     */
    public DaysHoursMinutes normalize(BigDecimal workingHoursPerDay) {
        BigDecimal totalHours_fromDays = days.multiply(workingHoursPerDay);
        BigDecimal totalHours_fromMinutes = minutes.divide(SIXTY, new MathContext(6, RoundingMode.HALF_EVEN));
        
        BigDecimal totalHours = hours.add(totalHours_fromDays).add(totalHours_fromMinutes);
        
        return convertToDaysHoursMinutes(totalHours, workingHoursPerDay);
    }
    
    /**
     * This method returns DaysHoursMinutes value in slash format.
     */
    @Override
    public String toString() {
        return String.format("%s/%s/%s", days.toPlainString(), hours.toPlainString(), minutes.toPlainString());
    }

    /**
     * Converts hours to d/h/m triple, using working hours per day.
     * 
     * @param hours
     * @param workingHoursPerDay
     * @return
     */
    static DaysHoursMinutes convertToDaysHoursMinutes(BigDecimal hours, BigDecimal workingHoursPerDay) {
        BigDecimal[] dh = hours.divideAndRemainder(workingHoursPerDay);
     
        BigDecimal d = dh[0];
        
        BigDecimal h = dh[1].setScale(0, RoundingMode.FLOOR);
        
        BigDecimal m = dh[1].subtract(h).multiply(BigDecimal.valueOf(60));
        
        m = m.setScale(0, RoundingMode.HALF_EVEN);
        
        return new DaysHoursMinutes(d, h, m);
    }
}
