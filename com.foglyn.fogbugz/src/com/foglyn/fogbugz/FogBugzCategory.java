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

import com.foglyn.fogbugz.FogBugzStatus.StatusID;

/**
 * Category of the case: Bug, Feature, Inquiry or Schedule Item (for now).
 * 
 * Category has name (and also name in plural form), <em>default</em> flag,
 * <em>schedule item</em> flag and <em>default status</em>.
 * 
 * <em>Default</em> flag indicates whether this category should be default when
 * creating new case.
 * 
 * <em>Schedule item</em> flag indicates whether this category is Schedule Item.
 * These are special because they were added to FogBugz in later version... we
 * will see what happens in the future, when more categories are added :-)
 * 
 * <em>Default status</em> is ID of {@link FogBugzStatus status} which should be
 * pre-selected when resolving case from this category. It is always defined.
 * 
 * Note: instances of this class are immutable.
 */
public final class FogBugzCategory implements HasID<FogBugzCategory.CategoryID> {
    public final static class CategoryID extends LongID {
        public static final CategoryID BUG = new CategoryID(1);
        public static final CategoryID FEATURE = new CategoryID(2);
        public static final CategoryID INQUIRY = new CategoryID(3);
        public static final CategoryID SCHEDULE_ITEM = new CategoryID(4);
        
        private CategoryID(long caseID) {
            super(caseID);
        }

        public static CategoryID valueOf(String ixCategory) {
            long c = Long.parseLong(ixCategory);
            if (c < 0) return null;
            
            if (c == 1) return BUG;
            if (c == 2) return FEATURE;
            if (c == 3) return INQUIRY;
            if (c == 4) return SCHEDULE_ITEM;
            
            return new CategoryID(c);
        }
    }

    public static class CategoryIDFactory implements IDFactory<CategoryID> {
        public CategoryID valueOf(String ixCategory) {
            return CategoryID.valueOf(ixCategory);
        }
    }
    
    private final CategoryID categoryID;
    private final String name;
    private final String pluralName;
    private final boolean isScheduleItem;
    private final StatusID defaultResolvedStatus;
    private final StatusID defaultActiveStatus;
    
    FogBugzCategory(CategoryID categoryID, String name, String pluralName, boolean isScheduleItem, StatusID defaultResolvedStatus, StatusID defaultActiveStatus) {
        Utils.assertNotNullArg(categoryID, "categoryID");
        Utils.assertNotNullArg(name, "name");
        Utils.assertNotNullArg(pluralName, "pluralName");
        Utils.assertNotNullArg(defaultResolvedStatus, "defaultStatus");
        // defaultActiveStatus can be null, it was introduced in FogBugz 7
        
        this.categoryID = categoryID;
        this.name = name;
        this.pluralName = pluralName;
        this.isScheduleItem = isScheduleItem;
        this.defaultResolvedStatus = defaultResolvedStatus;
        this.defaultActiveStatus = defaultActiveStatus;
    }
    
    public CategoryID getID() {
        return categoryID;
    }

    public String getName() {
        return name;
    }

    public String getPluralName() {
        return pluralName;
    }

    /**
     * @return true, if this is Schedule Item category
     */
    public boolean isScheduleItem() {
        return isScheduleItem;
    }

    /**
     * @return ID of {@link FogBugzStatus status} which should be pre-selected
     *         when resolving case from this category.
     */
    public StatusID getDefaultResolvedStatus() {
        return defaultResolvedStatus;
    }

    /**
     * @return ID of {@link FogBugzStatus status} which should be pre-selected
     *         when resolving case from this category.
     */
    public StatusID getDefaultActiveStatus() {
        return defaultActiveStatus;
    }
    
    @Override
    public String toString() {
        return "Category: " + name + " (" + categoryID + ")";
    }
}
