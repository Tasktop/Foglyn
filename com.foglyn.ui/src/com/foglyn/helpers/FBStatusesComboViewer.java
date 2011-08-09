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

package com.foglyn.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;

import com.foglyn.fogbugz.FogBugzCategory;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzStatus;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;

public class FBStatusesComboViewer extends ComboViewer {
    private FBStatusItemLabelProvider labelProvider;
    private FBStatusItemFilter statusFilter;
    
    public FBStatusesComboViewer(Composite parent, int style, String nullValueLabel) {
        super(parent, style);
        
        labelProvider = new FBStatusItemLabelProvider();
        
        setLabelProvider(new NullValueLabelProvider(nullValueLabel, labelProvider));
        setContentProvider(new CollectionContentProvider(true));

        statusFilter = new FBStatusItemFilter();
        addFilter(new NullValueFilter(statusFilter));
    }

    public void setsetCategoryID(CategoryID categoryID) {
        statusFilter.setCategoryID(categoryID);
    }

    public void setUsePrefix(boolean usePrefix) {
        labelProvider.setUsePrefix(usePrefix);
    }

    public void setStatusComboFromClientValues(FogBugzClient client) {
        Collection<FogBugzStatus> allStatuses = client.getAllStatuses();
        
        Map<CategoryID, List<FogBugzStatus>> statusMap = new LinkedHashMap<CategoryID, List<FogBugzStatus>>();
        List<FogBugzStatus> allCategoryStatus = new ArrayList<FogBugzStatus>();
        
        Set<FogBugzStatus> activeStatuses = new LinkedHashSet<FogBugzStatus>();
        Set<FogBugzStatus> resolvedStatuses = new LinkedHashSet<FogBugzStatus>();
        
        for (FogBugzStatus s: allStatuses) {
            CategoryID cat = s.getCategoryID();
            if (cat == null) {
                allCategoryStatus.add(s);
            } else {
                List<FogBugzStatus> catStats = statusMap.get(cat);
                if (catStats == null) {
                    catStats = new ArrayList<FogBugzStatus>();
                    statusMap.put(cat, catStats);
                }
                catStats.add(s);
            }
            
            if (s.isResolved()) {
                resolvedStatuses.add(s);
            } else {
                activeStatuses.add(s);
            }
        }
        
        for (List<FogBugzStatus> sl: statusMap.values()) {
            // Active < Resolved, Active == Active, Resolved == Resolved
            // This sort method doesn't 'merge' equal values
            Collections.sort(sl, new Comparator<FogBugzStatus>() {
                public int compare(FogBugzStatus o1, FogBugzStatus o2) {
                    if (o1.isResolved() == o2.isResolved()) {
                        return 0;
                    }
                    
                    if (!o1.isResolved() && o2.isResolved()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
        }
        
        // now we are ready to create values for combo boxes
        
        // -- Any -- (handled automatically)
        // -- Active (any category) --
        // -- Resolved (any category) --
        // Bug: Active
        // Bug: Resolved (Fixed)
        // Bug: ...
        // Feature: Active
        // Feature: ...
        // remaining categories here

        List<FBStatusItem> categorySpecific = new ArrayList<FBStatusItem>();
        
        Set<FogBugzStatus> categoryResolvedStatuses = new HashSet<FogBugzStatus>();
        for (Entry<CategoryID, List<FogBugzStatus>> e: statusMap.entrySet()) {
            String prefix = e.getKey().toString();

            FogBugzCategory cat = client.getCategory(e.getKey());
            if (cat != null) {
                prefix = cat.getName();
            }
            
            for (FogBugzStatus s: e.getValue()) {
                categorySpecific.add(new FBStatusItem(s.getName(), prefix, s.getID(), s.getCategoryID()));
                
                if (s.isResolved()) {
                    categoryResolvedStatuses.add(s);
                }
            }
        }

        List<FBStatusItem> comboItems = new ArrayList<FBStatusItem>();
        comboItems.add(new FBStatusItem("\u2014 Active \u2014", null, getStatusIDs(activeStatuses), null));
        comboItems.add(new FBStatusItem("\u2014 Resolved \u2014", null, getStatusIDs(resolvedStatuses), null));

        Set<FogBugzStatus> commonResolvedStatuses = new HashSet<FogBugzStatus>(resolvedStatuses);
        commonResolvedStatuses.removeAll(categoryResolvedStatuses);
        
        // resolved statuses common for all categories
        for (FogBugzStatus s: commonResolvedStatuses) {
            comboItems.add(new FBStatusItem(s.getName(), null, s.getID(), s.getCategoryID()));
        }

        // put category-specific statuses at the end
        comboItems.addAll(categorySpecific);
        
        // And now... set values in combobox, and select same value that was selected before updating
        Set<StatusID> selected = getSelectedStatuses();
        
        this.setInput(comboItems);

        FBStatusItem valueToSet = null;
        if (selected != null) {
            for (FBStatusItem fbsi: comboItems) {
                if (selected.equals(fbsi.getStatuses())) {
                    valueToSet = fbsi;
                }
            }
        }
        
        if (valueToSet != null) {
            this.setSelection(new StructuredSelection(valueToSet), true);
        } else {
            this.setSelection(HelperConstants.NULL_VALUE_SELECTION, true);
        }
    }

    private Set<StatusID> getStatusIDs(Collection<FogBugzStatus> statuses) {
        Set<StatusID> result = new HashSet<StatusID>();
        for (FogBugzStatus fbs: statuses) {
            result.add(fbs.getID());
        }
        
        return result;
    }

    public Set<StatusID> getSelectedStatuses() {
        StructuredSelection ssel = (StructuredSelection) getSelection();
        if (ssel.isEmpty()) {
            return null;
        }
        
        Assert.isTrue(ssel.size() == 1);
        
        Object obj = ssel.getFirstElement();
        if (HelperConstants.NULL_VALUE.equals(obj)) {
            return null;
        }
        
        FBStatusItem fbsi = (FBStatusItem) obj;
        return fbsi.getStatuses();
    }

    public void selectStatuses(Set<StatusID> toSelect) {
        if (toSelect == null) {
            setSelection(HelperConstants.NULL_VALUE_SELECTION, true);
            return;
        }
        
        Collection<FBStatusItem> items = (Collection<FBStatusItem>) getInput();
        for (FBStatusItem fbsi : items) {
            if (toSelect.equals(fbsi.getStatuses())) {
                setSelection(new StructuredSelection(fbsi), true);
                return;
            }
        }
    }
}
