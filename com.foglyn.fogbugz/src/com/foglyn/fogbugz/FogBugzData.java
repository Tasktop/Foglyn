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

package com.foglyn.fogbugz;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.foglyn.fogbugz.FogBugzArea;
import com.foglyn.fogbugz.FogBugzCategory;
import com.foglyn.fogbugz.FogBugzFixFor;
import com.foglyn.fogbugz.FogBugzPerson;
import com.foglyn.fogbugz.FogBugzPriority;
import com.foglyn.fogbugz.FogBugzProject;
import com.foglyn.fogbugz.FogBugzStatus;
import com.foglyn.fogbugz.HasID;
import com.foglyn.fogbugz.ID;
import com.foglyn.fogbugz.WorkingSchedule;
import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;

/**
 * This class holds information from FogBugz server.
 * 
 * Instances of this class are immutable. If you want to modify / create new instance, use FogBugzDataBuilder class.
 */
public class FogBugzData {
    private final Map<CategoryID, FogBugzCategory> categories;
    private final Map<PriorityID, FogBugzPriority> priorities;
    private final Map<StatusID, FogBugzStatus> statuses;
    
    /** Active or virtual people */
    private final Map<PersonID, FogBugzPerson> people;
    
    /** Inactive (deleted) people */
    private final Map<PersonID, FogBugzPerson> inactivePeople;
    
    /** People, which we tried to fetch from FogBugz (as inactive), but do not exist */
    private final Set<PersonID> nonExistantPeople;
    
    private final Map<ProjectID, FogBugzProject> projects;
    private final Map<AreaID, FogBugzArea> areas;
    private final Map<FixForID, FogBugzFixFor> fixFors;

    /**
     * Working schedule for logged-in user.
     */
    private final WorkingSchedule workingSchedule;
    
    /**
     * Working schedule for entire site. Used for converting hours to days.
     */
    private final WorkingSchedule siteWorkingSchedule;

    /**
     * ID of current user.
     */
    private final PersonID currentUser;
    
    private final Set<CacheType> loadCacheTypes;
    
    private FogBugzData(Map<CategoryID, FogBugzCategory> categories, Map<PriorityID, FogBugzPriority> priorities,
            Map<StatusID, FogBugzStatus> statuses, Map<PersonID, FogBugzPerson> people, Map<PersonID, FogBugzPerson> inactivePeople,
            Set<PersonID> nonExistantPeople, Map<ProjectID, FogBugzProject> projects, Map<AreaID, FogBugzArea> areas,
            Map<FixForID, FogBugzFixFor> fixFors, WorkingSchedule workingSchedule, WorkingSchedule siteWorkingSchedule,
            PersonID currentUser, Set<CacheType> loadCacheTypes) {
        this.categories = Collections.unmodifiableMap(new LinkedHashMap<CategoryID, FogBugzCategory>(categories));
        this.priorities = Collections.unmodifiableMap(new LinkedHashMap<PriorityID, FogBugzPriority>(priorities));
        this.statuses = Collections.unmodifiableMap(new LinkedHashMap<StatusID, FogBugzStatus>(statuses));
        
        this.people = Collections.unmodifiableMap(new LinkedHashMap<PersonID, FogBugzPerson>(people));
        this.inactivePeople = Collections.unmodifiableMap(new LinkedHashMap<PersonID, FogBugzPerson>(inactivePeople));
        this.nonExistantPeople = Collections.unmodifiableSet(new HashSet<PersonID>(nonExistantPeople));
        
        this.projects = Collections.unmodifiableMap(new LinkedHashMap<ProjectID, FogBugzProject>(projects));
        this.areas = Collections.unmodifiableMap(new LinkedHashMap<AreaID, FogBugzArea>(areas));
        this.fixFors = Collections.unmodifiableMap(new LinkedHashMap<FixForID, FogBugzFixFor>(fixFors));
        
        this.workingSchedule = workingSchedule;
        this.siteWorkingSchedule = siteWorkingSchedule;
        
        this.currentUser = currentUser;
        
        this.loadCacheTypes = Collections.unmodifiableSet(EnumSet.copyOf(loadCacheTypes));
    }

    public FogBugzCategory getCategory(CategoryID categoryID) {
        return categories.get(categoryID);
    }
    
    public Collection<FogBugzCategory> getAllCategories() {
        return categories.values();
    }

    public FogBugzPriority getPriority(PriorityID priorityID) {
        return priorities.get(priorityID);
    }

    public Collection<FogBugzPriority> getAllPriorities() {
        return priorities.values();
    }
    
    public FogBugzStatus getStatus(StatusID statusID) {
        return statuses.get(statusID);
    }

    public Collection<FogBugzStatus> getAllStatuses() {
        return statuses.values();
    }
    
    public FogBugzProject getProject(ProjectID projectID) {
        return projects.get(projectID);
    }
    
    public Collection<FogBugzProject> getAllProjects() {
        return this.projects.values();
    }
    
    public FogBugzPerson getPerson(PersonID personID) {
        FogBugzPerson result = people.get(personID);
        if (result == null) {
            result = inactivePeople.get(personID);
        }
        return result;
    }
    
    public boolean isNonExistant(PersonID personID) {
        return nonExistantPeople.contains(personID);
    }
    
    /**
     * @return collection of active people (normal or virtual). Inactive people
     *         are not returned, even if they were already fetched from server.
     */
    public Collection<FogBugzPerson> getAllPeople() {
        return people.values();
    }
    
    public Collection<FogBugzPerson> getAllInactivePeople() {
        return inactivePeople.values();
    }
    
    public FogBugzArea getArea(AreaID areaID) {
        return areas.get(areaID);
    }
    
    public Collection<FogBugzArea> getAllAreas() {
        return this.areas.values();
    }
    
    public FogBugzFixFor getFixFor(FixForID fixFor) {
        return fixFors.get(fixFor);
    }
    
    public Collection<FogBugzFixFor> getAllFixFors() {
        return this.fixFors.values();
    }
    
    /**
     * @return working schedule for logged-in user (may be null, if caches has not yet been loaded)
     */
    public WorkingSchedule getWorkingSchedule() {
        return workingSchedule;
    }
    
    public WorkingSchedule getSiteWorkingSchedule() {
        return siteWorkingSchedule;
    }

    public PersonID getOwner(ProjectID projectID, AreaID areaID) {
        if (areaID != null) {
            FogBugzArea a = this.areas.get(areaID);
            if (a != null) {
                if (a.getOwner() != null) {
                    return a.getOwner();
                }
            }
        }
        
        if (projectID != null) {
            FogBugzProject p = this.projects.get(projectID);
            if (p != null) {
                if (p.getOwner() != null) {
                    return p.getOwner();
                }
            }
        }
        
        return null;
    }

    public PersonID getCurrentUser() {
        return currentUser;
    }

    public Set<CacheType> getLoadCacheTypes() {
        return loadCacheTypes;
    }
    
    public static class FogBugzDataBuilder {
        private Map<CategoryID, FogBugzCategory> categories = new LinkedHashMap<CategoryID, FogBugzCategory>();
        private Map<PriorityID, FogBugzPriority> priorities = new LinkedHashMap<PriorityID, FogBugzPriority>();
        private Map<StatusID, FogBugzStatus> statuses = new LinkedHashMap<StatusID, FogBugzStatus>();
        
        private Map<PersonID, FogBugzPerson> people = new LinkedHashMap<PersonID, FogBugzPerson>();
        private Map<PersonID, FogBugzPerson> inactivePeople = new LinkedHashMap<PersonID, FogBugzPerson>();
        private Set<PersonID> nonExistantPeople = new HashSet<PersonID>();
        
        private Map<ProjectID, FogBugzProject> projects = new LinkedHashMap<ProjectID, FogBugzProject>();
        private Map<AreaID, FogBugzArea> areas = new LinkedHashMap<AreaID, FogBugzArea>();
        private Map<FixForID, FogBugzFixFor> fixFors = new LinkedHashMap<FixForID, FogBugzFixFor>();

        private WorkingSchedule workingSchedule;
        private WorkingSchedule siteWorkingSchedule;
        
        private PersonID currentUser;
        
        private Set<CacheType> loadCacheTypes = EnumSet.noneOf(CacheType.class);
        
        private <K extends ID, T extends HasID<K>> void add(Map<K, T> map, T value) {
            map.put(value.getID(), value);
        }

        private <K extends ID, T extends HasID<K>> void addAll(Map<K, T> map, Collection<T> values) {
            for (T val: values) {
                map.put(val.getID(), val);
            }
        }
        
        public FogBugzDataBuilder() {
            // empty builder
        }
        
        public FogBugzDataBuilder(FogBugzData data) {
            addAll(categories, data.getAllCategories());
            addAll(priorities, data.getAllPriorities());
            addAll(statuses, data.getAllStatuses());
            addAll(people, data.getAllPeople());
            addAll(inactivePeople, data.getAllInactivePeople());
            nonExistantPeople.addAll(data.nonExistantPeople);

            addAll(projects, data.getAllProjects());
            addAll(areas, data.getAllAreas());
            addAll(fixFors, data.getAllFixFors());

            workingSchedule = data.getWorkingSchedule();
            siteWorkingSchedule = data.getSiteWorkingSchedule();
            currentUser = data.getCurrentUser();
            loadCacheTypes.addAll(data.getLoadCacheTypes());
        }

        FogBugzData build() {
            return new FogBugzData(categories, priorities, statuses, people,
                    inactivePeople, nonExistantPeople, projects, areas,
                    fixFors, workingSchedule, siteWorkingSchedule, currentUser, loadCacheTypes);
        }

        void addCategories(Collection<FogBugzCategory> categories) {
            addAll(this.categories, categories);
        }
        
        void addPriorities(Collection<FogBugzPriority> priorities) {
            addAll(this.priorities, priorities);
        }
        
        void addStatuses(Collection<FogBugzStatus> statuses) {
            addAll(this.statuses, statuses);
        }
        
        void addPeople(Collection<FogBugzPerson> allPeople) {
            addAll(this.people, allPeople);
        }

        void addAreas(Collection<FogBugzArea> allAreas) {
            addAll(this.areas, allAreas);
        }

        void addProjects(Collection<FogBugzProject> allProjects) {
            addAll(this.projects, allProjects);
        }

        void addFixFors(Collection<FogBugzFixFor> allFixFors) {
            addAll(this.fixFors, allFixFors);
        }

        void setWorkingSchedule(WorkingSchedule workingSchedule) {
            this.workingSchedule = workingSchedule;
        }
        
        void setSiteWorkingSchedule(WorkingSchedule siteWorkingSchedule) {
            this.siteWorkingSchedule = siteWorkingSchedule;
        }

        void addInactivePerson(FogBugzPerson person) {
            add(this.inactivePeople, person);
        }
        
        void addNonExistantPersonID(PersonID personID) {
            this.nonExistantPeople.add(personID);
        }

        void addPerson(FogBugzPerson person) {
            add(this.people, person);
        }
        
        void setCurrentUser(PersonID personID) {
            this.currentUser = personID;
        }
        
        void addLoadCacheTypes(Set<CacheType> loadCacheTypes) {
            this.loadCacheTypes.addAll(loadCacheTypes);
        }
    }
}
