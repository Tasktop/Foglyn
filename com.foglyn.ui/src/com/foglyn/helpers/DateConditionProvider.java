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

package com.foglyn.helpers;

import com.foglyn.core.AdvancedSearchQuery.DateCondition;

public class DateConditionProvider extends TypedLabelProvider<DateCondition> {
    public DateConditionProvider() {
        super(DateCondition.class);
    }

    @Override
    protected String getTextForElement(DateCondition element) {
        switch (element) {
        case TODAY: return "today";
        case TODAY_OR_TOMORROW: return "today or tomorrow";
        case TODAY_OR_YESTERDAY: return "yesterday or today";
        case LAST_2_MONTHS: return "in the last two months";
        case LAST_3_MONTHS: return "in the last three months";
        case LAST_6_MONTHS: return "in the last six months";
        case LAST_MONTH: return "in the last month";
        case LAST_WEEK: return "in the last week";
        case LAST_YEAR: return "in the last year";
        case NEXT_2_MONTHS: return "in the next two months";
        case NEXT_3_MONTHS: return "in the next three months";
        case NEXT_6_MONTHS: return "in the next six months";
        case NEXT_MONTH: return "in the next month";
        case NEXT_WEEK: return "in the next week";
        case NEXT_YEAR: return "in the next year";
        case IN_THE_PAST: return "in the past";
        default: return null;
        }
    }
}
