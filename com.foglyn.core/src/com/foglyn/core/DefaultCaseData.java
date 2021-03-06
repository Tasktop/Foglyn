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

package com.foglyn.core;

import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;

/**
 * This class holds default data which is used when creating new case.
 */
class DefaultCaseData {
    private ProjectID defaultProject;
    private AreaID defaultArea;
    private PersonID defaultOwner;
    private CategoryID defaultCategory;
    private PriorityID defaultPriority;
    private FixForID defaultFixFor;

    public ProjectID getDefaultProject() {
        return defaultProject;
    }

    public void setDefaultProject(ProjectID defaultProject) {
        this.defaultProject = defaultProject;
    }

    public AreaID getDefaultArea() {
        return defaultArea;
    }

    public void setDefaultArea(AreaID defaultArea) {
        this.defaultArea = defaultArea;
    }

    public PersonID getDefaultOwner() {
        return defaultOwner;
    }

    public void setDefaultOwner(PersonID defaultOwner) {
        this.defaultOwner = defaultOwner;
    }

    public CategoryID getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(CategoryID defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public PriorityID getDefaultPriority() {
        return defaultPriority;
    }

    public void setDefaultPriority(PriorityID defaultPriority) {
        this.defaultPriority = defaultPriority;
    }

    public FixForID getDefaultFixFor() {
        return defaultFixFor;
    }

    public void setDefaultFixFor(FixForID defaultFixFor) {
        this.defaultFixFor = defaultFixFor;
    }
}
