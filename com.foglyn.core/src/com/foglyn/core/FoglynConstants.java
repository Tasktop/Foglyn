package com.foglyn.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class FoglynConstants {
    /**
     * Single select with support for dependencies.
     */
    public static final String TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES = "foglyn_singleSelectWithDependancies";
    public static final String TYPE_OPTION_VALUE = "foglyn_optionValue";
    
    // Custom dateTime type, because Mylyn's one is available since 3.1 only.
    public static final String TYPE_CUSTOM_DATETIME = "foglyn_dateTime";
    
    /**
     * Text editor which parses days/hours/minutes and checks for syntax
     */
    public static final String TYPE_DAYS_HOURS_MINUTES = "foglyn_daysHoursMinutes";

    /**
     * Read-only hyperlinked text
     */
    public static final String TYPE_RELATED_CASES = "foglyn_relatedCases";
    
    /**
     * Attributes with this metadata will act as "master value" for its dependent attributes. Value of this metadata is one of dependency key. {@link Dependency}
     */
    public static final String META_DEPENDENCY_KEY = "foglyn_dependencyKey";
    
    /**
     * Specifies dependencies for this attribute (comma separated values).
     */
    public static final String META_DEPENDS_ON = "foglyn_dependsOn";
    
    /**
     * Used in option values for custom single select. Specifies that this option can be used as default value. (Boolean metadata)
     */
    public static final String META_DEFAULT_VALUE = "foglyn_defaultValue";
    
    /**
     * Boolean metadata (when missing, default it 'true'). When false, default value is not set in single select with dependencies attributes.
     */
    public static final String META_SET_DEFAULT = "foglyn_setDefault";
    
    /**
     * Value (id) for attribute option. This is optional, and when missing, attribute ID is used as option value.
     */
    public static final String META_VALUE_ID = "foglyn_valueID";
    
    /**
     * Metadata which specifies if this TaskOperation can reassign case. When not specified, default is false.
     */
    public static final String META_OPERATION_CAN_REASSIGN = "foglyn_canReassign";
    
    /**
     * Decimal value indicating working hours per day. Stored in editable DayHoursMinutes types.
     */
    public static final String META_WORKING_HOURS_PER_DAY = "foglyn_workingHoursPerDay";

    /**
     * "Military" hour (as FB people call it) when work day starts. This is default due time. (Isn't local hour, but GMT hour)
     */
    public static final String META_WORKDAY_START = "foglyn_workdayStart";
    
    /**
     * Indicates that value was not defined in the case previously. Useful for estimate or due time, when we want to distinguish between
     * 1) there was no value before, and none is entered, and 2) there was value before, and user cleared it.
     */
    public static final String META_NON_EMPTY_PREVIOUS_VALUE = "foglyn_validPreviousValue";
    
    /**
     * Indicates category of the task. Stored in task's attributes (also in task data).
     */
    public static final String TASK_ATTRIBUTE_CATEGORY = "fb_ixCategory";
    
    public static final String TASK_ATTRIBUTE_TIME_TRACKING_ENABLED = "foglyn_timeTrackingEnabled";

    /**
     * Timestamp (long/ms) of last full update. Used to report change when full update is coming,
     * and local task was not fully updated recently.
     */
    public static final String TASK_ATTRIBUTE_FULL_TASKDATA_LAST_UPDATE = "foglyn_fullTaskDataLastUpdateTime";

    /**
     * Used to mark description part of attachment at the beginning of case comment.
     */
    public static final String ATTACHMENT_DESCRIPTION_PREFIX = "Attachment: ";

    /**
     * Used to mark description part of attached patch at the beginning of case comment.
     */
    public static final String PATCH_DESCRIPTION_PREFIX = "Patch: ";
    
    /**
     * Used as property in TaskRepository. Value is "true" or "false".
     * Also used as property in ITask
     */
    public static final String REPOSITORY_IS_FOGBUGZ7_REPOSITORY = "foglyn_isFogBugz7Repository";
    
    /**
     * Repository property that specifies which cases are treated as 'complete'.
     */
    public static final String REPOSITORY_COMPLETED_CASE_MODE = "foglyn_completedCaseMode";

    /**
     * Repository property that specifies whether active cases are synchronized with 'Working On' feature of FogBugz.
     */
    public static final String REPOSITORY_SYNCHRONIZE_WORKING_ON = "foglyn_synchronizeWorkingOn";
    
    public enum Dependency {
        PROJECT("project"),
        AREA("area"),
        CATEGORY("category");
        
        private final String key;
        
        Dependency(String key) {
            this.key = key;
        }
        
        public String getKey() {
            return key;
        }

        public static Dependency fromKey(String depKey) {
            for (Dependency d: values()) {
                if (d.key.equals(depKey)) return d;
            }
            
            throw new IllegalArgumentException("Illegal dependency key: " + depKey);
        }
    }
    
    public static String createDependsOn(Set<Dependency> deps) {
        StringBuilder sb = new StringBuilder();
        String sep="";
        for (Dependency d: deps) {
            sb.append(sep);
            sb.append(d.getKey());
            sep=",";
        }
        
        return sb.toString();
    }
    
    public static Set<Dependency> parseDependsOn(String dependsOn) {
        if (dependsOn == null) {
            return Collections.emptySet();
        }
        
        Set<Dependency> result = EnumSet.noneOf(Dependency.class);
        String[] deps = dependsOn.split(",");
        for (String d: deps) {
            result.add(Dependency.fromKey(d));
        }
        return result;
    }
}
