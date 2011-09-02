/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

package com.foglyn.fogbugz;

import java.math.BigDecimal;

public class WorkingSchedule {
    // Hour when workday starts. It seems that this is UTC-based hour.
    private BigDecimal workdayStart;
    
    // Hour when workday ends. When this is before start, it means night shift.
    private BigDecimal workdayEnd;
    
    private boolean hasLunch;

    // When does lunch starts?
    private BigDecimal lunchStart;
    
    // Length of lunch in hours
    private BigDecimal lunchLenghtHours;
    
    public BigDecimal getWorkdayStart() {
        return workdayStart;
    }

    public void setWorkdayStart(BigDecimal workdayStart) {
        this.workdayStart = workdayStart;
    }

    public BigDecimal getWorkdayEnd() {
        return workdayEnd;
    }

    public void setWorkdayEnd(BigDecimal workdayEnd) {
        this.workdayEnd = workdayEnd;
    }
    
    public boolean getHasLunch() {
        return hasLunch;
    }

    public void setHasLunch(boolean hasLunch) {
        this.hasLunch = hasLunch;
    }

    public BigDecimal getLunchLenghtHours() {
        return lunchLenghtHours;
    }

    public void setLunchLenghtHours(BigDecimal lunchLenghtHours) {
        this.lunchLenghtHours = lunchLenghtHours;
    }

    public BigDecimal getLunchStart() {
        return lunchStart;
    }

    public void setLunchStart(BigDecimal lunchStart) {
        this.lunchStart = lunchStart;
    }

    public BigDecimal getWorkingHoursPerDay() {
        BigDecimal start = workdayStart;
        BigDecimal end = workdayEnd;
        
        if (start.compareTo(end) > 0) {
            start = start.subtract(new BigDecimal("24"));
        }
        
        BigDecimal workingHours = end.subtract(start);
        if (hasLunch) {
            workingHours = workingHours.subtract(lunchLenghtHours);
        }
        
        return workingHours;
    }
}
