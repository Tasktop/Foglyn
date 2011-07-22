package com.foglyn.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

import com.foglyn.fogbugz.DateHelper;
import com.foglyn.fogbugz.FogBugzArea.AreaID;
import com.foglyn.fogbugz.FogBugzArea.AreaIDFactory;
import com.foglyn.fogbugz.FogBugzCase;
import com.foglyn.fogbugz.FogBugzCategory.CategoryID;
import com.foglyn.fogbugz.FogBugzCategory.CategoryIDFactory;
import com.foglyn.fogbugz.FogBugzClient;
import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzFixFor.FixForID;
import com.foglyn.fogbugz.FogBugzFixFor.FixForIDFactory;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;
import com.foglyn.fogbugz.FogBugzPerson.PersonIDFactory;
import com.foglyn.fogbugz.FogBugzPriority.PriorityID;
import com.foglyn.fogbugz.FogBugzPriority.PriorityIDFactory;
import com.foglyn.fogbugz.FogBugzProject.ProjectID;
import com.foglyn.fogbugz.FogBugzProject.ProjectIDFactory;
import com.foglyn.fogbugz.FogBugzStatus.StatusID;
import com.foglyn.fogbugz.ID;
import com.foglyn.fogbugz.IDFactory;
import com.foglyn.fogbugz.SearchQueryBuilder;

// setters accept null values
public class AdvancedSearchQuery extends FoglynQuery {
    public enum DateCondition {
        TODAY,
        TODAY_OR_YESTERDAY,
        
        LAST_WEEK,
        LAST_MONTH,
        LAST_2_MONTHS,
        LAST_3_MONTHS,
        LAST_6_MONTHS,
        LAST_YEAR,
        
        // due time
        IN_THE_PAST,
        TODAY_OR_TOMORROW,
        NEXT_WEEK,
        NEXT_MONTH,
        NEXT_2_MONTHS,
        NEXT_3_MONTHS,
        NEXT_6_MONTHS,
        NEXT_YEAR
    }
    
    private static final String QUERY_TYPE = "advanced-search";

    private static final String SEARCH_STRING = "searchString";
    private static final String PRIORITY = "priority";
    private static final String FIX_FOR = "fixFor";
    private static final String STATUS = "status";
    private static final String AREA = "area";
    private static final String PROJECT = "project";
    private static final String CATEGORY = "category";
    private static final String CLOSED_BY = "closedBy";
    private static final String OPENED_BY = "openedBy";
    private static final String RESOLVED_BY = "resolvedBy";
    private static final String ASSIGNED_TO = "assignedTo";
    private static final String ASSIGNED_TO_ME = "assignedToMe";
    private static final String STARRED_BY_ME = "starredByMe";
    private static final String SUBSCRIBED_BY_ME = "subscribedByMe";
    private static final String CORRESPONDENT = "correspondent";
    private static final String CLOSED = "closed";
    private static final String RESOLVED = "resolved";
    private static final String LAST_EDITED = "lastEdited";
    private static final String EDITED = "edited";
    private static final String OPENED = "opened";
    private static final String DUE = "due";
    private static final String ALSO_EDITED_BY = "alsoEditedBy";
    private static final String LAST_EDITED_BY = "lastEditedBy";
    private static final String EDITED_BY = "editedBy";
    private static final String TAGS = "tags";
    
    static boolean matches(String queryType) {
        return QUERY_TYPE.equals(queryType);
    }
    
    private String searchString;

    private PersonID assignedTo;
    private PersonID resolvedBy;
    private PersonID openedBy;
    private PersonID editedBy;
    private PersonID lastEditedBy;
    private PersonID alsoEditedBy;
    private PersonID closedBy;
    private CategoryID category;
    private ProjectID project;
    private AreaID area;
    private Set<StatusID> statuses;
    private FixForID fixFor;
    private PriorityID priority;
    
    private DateCondition due;
    private DateCondition opened;
    private DateCondition edited;
    private DateCondition lastEdited;
    private DateCondition resolved;
    private DateCondition closed;

    private String correspondent;
    
    private boolean subscribedByMe;
    private boolean starredByMe;
    
    private List<String> tags;

    private boolean assignedToMe;

    // RelatedTo (case number)
    
    @Override
    String getQueryType() {
        return QUERY_TYPE;
    }

    @Override
    void loadQueryParameters(IRepositoryQuery query) {
        searchString = query.getAttribute(SEARCH_STRING);
        assignedTo = getID(query, ASSIGNED_TO, new PersonIDFactory());
        resolvedBy = getID(query, RESOLVED_BY, new PersonIDFactory());
        openedBy = getID(query, OPENED_BY, new PersonIDFactory());
        closedBy = getID(query, CLOSED_BY, new PersonIDFactory());
        editedBy = getID(query, EDITED_BY, new PersonIDFactory());
        lastEditedBy = getID(query, LAST_EDITED_BY, new PersonIDFactory());
        alsoEditedBy = getID(query, ALSO_EDITED_BY, new PersonIDFactory());
        category = getID(query, CATEGORY, new CategoryIDFactory());
        project = getID(query, PROJECT, new ProjectIDFactory());
        area = getID(query, AREA, new AreaIDFactory());
        statuses = getStatuses(query.getAttribute(STATUS));
        fixFor = getID(query, FIX_FOR, new FixForIDFactory());
        priority = getID(query, PRIORITY, new PriorityIDFactory());
        
        due = getDateCondition(query, DUE);
        opened = getDateCondition(query, OPENED);
        edited = getDateCondition(query, EDITED);
        lastEdited = getDateCondition(query, LAST_EDITED);
        resolved = getDateCondition(query, RESOLVED);
        closed = getDateCondition(query, CLOSED);

        correspondent = query.getAttribute(CORRESPONDENT);
        subscribedByMe = Boolean.parseBoolean(query.getAttribute(SUBSCRIBED_BY_ME));
        starredByMe = Boolean.parseBoolean(query.getAttribute(STARRED_BY_ME));
        assignedToMe = Boolean.parseBoolean(query.getAttribute(ASSIGNED_TO_ME));
        
        String tagsAttr = query.getAttribute(TAGS);
        if (tagsAttr != null) {
            tags = Utils.getTagsFromString(tagsAttr);
        } else {
            tags = Collections.emptyList();
        }
    }
    
    private static Set<StatusID> getStatuses(String attribute) {
        if (Utils.isEmpty(attribute)) {
            return null;
        }
        
        String[] xs = attribute.split(",");
        
        Set<StatusID> result = new HashSet<StatusID>();
        for (String s: xs) {
            result.add(StatusID.valueOf(s));
        }
        
        if (result.isEmpty()) {
            return null;
        }
        
        return result;
    }

    private <T extends ID> T getID(IRepositoryQuery query, String attrName, IDFactory<T> factory) {
        String value = query.getAttribute(attrName);

        if (value == null) { return null; }
        return factory.valueOf(value);
    }
    
    private DateCondition getDateCondition(IRepositoryQuery query, String attrName) {
        String value = query.getAttribute(attrName);
        
        if (value == null) { return null; }
        return DateCondition.valueOf(value);
    }

    @Override
    void saveQueryParameters(IRepositoryQuery query) {
        query.setAttribute(SEARCH_STRING, searchString);
    
        saveAttribute(query, ASSIGNED_TO, assignedTo);
        saveAttribute(query, RESOLVED_BY, resolvedBy);
        saveAttribute(query, OPENED_BY, openedBy);
        saveAttribute(query, CLOSED_BY, closedBy);
        saveAttribute(query, EDITED_BY, editedBy);
        saveAttribute(query, LAST_EDITED_BY, lastEditedBy);
        saveAttribute(query, ALSO_EDITED_BY, alsoEditedBy);
        saveAttribute(query, CATEGORY, category);
        saveAttribute(query, PROJECT, project);
        saveAttribute(query, AREA, area);

        if (statuses == null || statuses.isEmpty()) {
            query.setAttribute(STATUS, null);
        } else {
            query.setAttribute(STATUS, Utils.join(statuses, "", ",", ""));
        }
        
        saveAttribute(query, FIX_FOR, fixFor);
        saveAttribute(query, PRIORITY, priority);
        
        saveDateCondition(query, DUE, due);
        saveDateCondition(query, OPENED, opened);
        saveDateCondition(query, EDITED, edited);
        saveDateCondition(query, LAST_EDITED, lastEdited);
        saveDateCondition(query, RESOLVED, resolved);
        saveDateCondition(query, CLOSED, closed);

        query.setAttribute(CORRESPONDENT, correspondent);
        query.setAttribute(SUBSCRIBED_BY_ME, Boolean.toString(subscribedByMe));
        query.setAttribute(STARRED_BY_ME, Boolean.toString(starredByMe));
        query.setAttribute(ASSIGNED_TO_ME, Boolean.toString(assignedToMe));
        
        if (tags == null || tags.isEmpty()) {
            query.setAttribute(TAGS, null);
        } else {
            query.setAttribute(TAGS, Utils.convertToTagsValue(tags));
        }
    }
    
    private void saveDateCondition(IRepositoryQuery query, String attrName, DateCondition condition) {
        if (condition != null) {
            query.setAttribute(attrName, condition.name());
        } else {
            query.setAttribute(attrName, null);
        }
    }

    private void saveAttribute(IRepositoryQuery query, String attrName, ID value) {
        if (value != null) {
            query.setAttribute(attrName, value.toString());
        } else {
            query.setAttribute(attrName, null);
        }
    }

    @Override
    public List<FogBugzCase> search(FogBugzClient client, IProgressMonitor monitor) throws FogBugzException {
        SearchQueryBuilder builder = new SearchQueryBuilder(client);
        
        if (searchString != null) {
            builder.setFreeFormQuery(searchString);
        }
        
        if (assignedTo != null) {
            builder.assignedTo(assignedTo);
        }
        
        if (assignedToMe) {
            builder.assignedToMe();
        }
        
        if (resolvedBy != null) {
            builder.resolvedBy(resolvedBy);
        }
        
        if (openedBy != null) {
            builder.openedBy(openedBy);
        }

        if (closedBy != null) {
            builder.closedBy(closedBy);
        }

        if (editedBy != null) {
            builder.editedBy(editedBy);
        }

        if (lastEditedBy != null) {
            builder.lastEditedBy(lastEditedBy);
        }

        if (alsoEditedBy != null) {
            builder.alsoEditedBy(alsoEditedBy);
        }
        
        if (category != null) {
            builder.category(category);
        }
        
        if (project != null) {
            builder.project(project);
        }
        
        if (area != null) {
            builder.area(area);
        }

        if (statuses != null && !statuses.isEmpty()) {
            builder.status(statuses.toArray(new StatusID[statuses.size()]));
        }
        
        if (fixFor != null) {
            builder.fixFor(fixFor);
        }
        
        if (priority != null) {
            builder.priority(priority);
        }

        if (due != null) {
            builder.due(convertDateCondition(due));
        }

        if (opened != null) {
            builder.opened(convertDateCondition(opened));
        }

        if (edited != null) {
            builder.edited(convertDateCondition(edited));
        }
        
        if (lastEdited != null) {
            builder.lastEdited(convertDateCondition(lastEdited));
        }

        if (resolved != null) {
            builder.resolved(convertDateCondition(resolved));
        }
        
        if (closed != null) {
            builder.closed(convertDateCondition(closed));
        }

        if (correspondent != null) {
            builder.correspondent(correspondent);
        }
        
        if (starredByMe) {
            builder.starredByMe();
        }
        
        if (subscribedByMe) {
            builder.subscribed();
        }
        
        if (client.isFogBugz7Repository() && tags != null && !tags.isEmpty()) {
            builder.tags(tags);
        }
        
        String q = builder.buildQuery();
        return client.search(q, monitor, false);
    }

    String convertDateCondition(DateCondition dc) {
        DateHelper helper = new DateHelper();
        
        switch (dc) {
        case TODAY: return helper.today();
        case TODAY_OR_TOMORROW: return helper.range(helper.today(), helper.tomorrow());
        case TODAY_OR_YESTERDAY: return helper.range(helper.yesterday(), helper.today());
        case IN_THE_PAST: return helper.range(null, helper.yesterday());
        case LAST_WEEK: return helper.range(helper.anotherDay(-7), helper.today());
        case LAST_MONTH: return helper.range(helper.anotherDay(-30), helper.today());
        case LAST_2_MONTHS: return helper.range(helper.anotherDay(-61), helper.today());
        case LAST_3_MONTHS: return helper.range(helper.anotherDay(-92), helper.today());
        case LAST_6_MONTHS: return helper.range(helper.anotherDay(-182), helper.today());
        case LAST_YEAR: return helper.range(helper.anotherDay(-365), helper.today());

        case NEXT_WEEK: return helper.range(helper.today(), helper.anotherDay(7));
        case NEXT_MONTH: return helper.range(helper.today(), helper.anotherDay(30));
        case NEXT_2_MONTHS: return helper.range(helper.today(), helper.anotherDay(61));
        case NEXT_3_MONTHS: return helper.range(helper.today(), helper.anotherDay(92));
        case NEXT_6_MONTHS: return helper.range(helper.today(), helper.anotherDay(182));
        case NEXT_YEAR: return helper.range(helper.today(), helper.anotherDay(365));
        
        default:
            throw new IllegalArgumentException("Unknown date condition: " + dc);
        }
    }
    
    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public void setCategory(CategoryID category) {
        this.category = category;
    }

    public void setProject(ProjectID project) {
        this.project = project;
    }

    public void setArea(AreaID area) {
        this.area = area;
    }

    public void setAssignedTo(PersonID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public void setResolvedBy(PersonID resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public void setOpenedBy(PersonID openedBy) {
        this.openedBy = openedBy;
    }

    public void setClosedBy(PersonID closedBy) {
        this.closedBy = closedBy;
    }

    public void setStatuses(Set<StatusID> statuses) {
        this.statuses = statuses;
    }

    public void setFixFor(FixForID fixFor) {
        this.fixFor = fixFor;
    }

    public void setPriority(PriorityID priority) {
        this.priority = priority;
    }

    public void setDue(DateCondition due) {
        this.due = due;
    }

    public void setOpened(DateCondition opened) {
        this.opened = opened;
    }

    public void setEdited(DateCondition edited) {
        this.edited = edited;
    }

    public void setLastEdited(DateCondition lastEdited) {
        this.lastEdited = lastEdited;
    }

    public void setResolved(DateCondition resolved) {
        this.resolved = resolved;
    }

    public void setClosed(DateCondition closed) {
        this.closed = closed;
    }

    public void setEditedBy(PersonID editedBy) {
        this.editedBy = editedBy;
    }
    
    public void setLastEditedBy(PersonID lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }
    
    public void setAlsoEditedBy(PersonID alsoEditedBy) {
        this.alsoEditedBy = alsoEditedBy;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribedByMe = subscribed;
    }
    
    public void setStarredByMe(boolean starredByMe) {
        this.starredByMe = starredByMe;
    }
    
    public void setCorrespondent(String correspondent) {
        this.correspondent = correspondent;
    }

    public boolean getSubscribedByMe() {
        return subscribedByMe;
    }

    public void setSubscribedByMe(boolean subscribedByMe) {
        this.subscribedByMe = subscribedByMe;
    }

    public String getSearchString() {
        return searchString;
    }

    public PersonID getAssignedTo() {
        return assignedTo;
    }

    public PersonID getResolvedBy() {
        return resolvedBy;
    }

    public PersonID getOpenedBy() {
        return openedBy;
    }

    public PersonID getEditedBy() {
        return editedBy;
    }

    public PersonID getLastEditedBy() {
        return lastEditedBy;
    }

    public PersonID getAlsoEditedBy() {
        return alsoEditedBy;
    }

    public PersonID getClosedBy() {
        return closedBy;
    }

    public CategoryID getCategory() {
        return category;
    }

    public AreaID getArea() {
        return area;
    }

    public Set<StatusID> getStatuses() {
        return statuses;
    }

    public FixForID getFixFor() {
        return fixFor;
    }

    public ProjectID getProject() {
        return project;
    }

    public PriorityID getPriority() {
        return priority;
    }

    public DateCondition getDue() {
        return due;
    }

    public DateCondition getOpened() {
        return opened;
    }

    public DateCondition getEdited() {
        return edited;
    }

    public DateCondition getLastEdited() {
        return lastEdited;
    }

    public DateCondition getResolved() {
        return resolved;
    }

    public DateCondition getClosed() {
        return closed;
    }

    public String getCorrespondent() {
        return correspondent;
    }

    public boolean getStarredByMe() {
        return starredByMe;
    }
    
    public String getTags() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        
        return Utils.convertToTagsValue(tags);
    }
    
    public void setTags(String tags) {
        if (tags != null) {
            this.tags = Utils.getTagsFromString(tags);
        } else {
            this.tags = null;
        }
    }

    public void setAssignedToMe(boolean assignedToMe) {
        this.assignedToMe = assignedToMe;
    }
}
