package com.foglyn.core;

import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;

public class FoglynTaskAttributeMapper extends TaskAttributeMapper {

    public FoglynTaskAttributeMapper(TaskRepository taskRepository) {
        super(taskRepository);
    }

    @Override
    public String mapToRepositoryKey(TaskAttribute parent, String key) {
        return mapKey(parent, key);
    }

    private String mapKey(TaskAttribute parent, String key) {
        if (key.equals(TaskAttribute.TASK_KEY)) {
            return FoglynAttribute.BUG_ID.getKey();
        }
        
        if (key.equals(TaskAttribute.SUMMARY)) {
            return FoglynAttribute.TITLE.getKey();
        }
        
        if (key.equals(TaskAttribute.DATE_MODIFICATION)) {
            return FoglynAttribute.LAST_UPDATED_DATE.getKey();
        }
        
        if (key.equals(TaskAttribute.DATE_CREATION)) {
            return FoglynAttribute.OPENED_DATE.getKey();
        }
        
        if (key.equals(TaskAttribute.DATE_COMPLETION)) {
            return FoglynAttribute.CLOSED_DATE.getKey();
        }
        
        if (key.equals(TaskAttribute.USER_ASSIGNED)) {
            return FoglynAttribute.ASSIGNED_TO_PERSON_ID.getKey();
        }
        
//        if (key.equals(TaskAttribute.PRIORITY)) {
//            return FoglynAttribute.PRIORITY_ID.getKey();
//        }
        
//        if (key.equals(TaskAttribute.PRODUCT)) {
//            return FoglynAttribute.PROJECT_ID.getKey();
//        }
        
//        if (key.equals(TaskAttribute.STATUS)) {
//            return FoglynAttribute.STATUS.getKey();
//        }

        if (key.equals(TaskAttribute.TASK_KIND)) {
            return FoglynAttribute.CATEGORY_ID.getKey();
        }

        if (key.equals(TaskAttribute.DATE_DUE)) {
            return FoglynAttribute.DUE_DATE.getKey();
        }

// Don't map description to NEW_COMMENT, it isn't the same.
//        if (key.equals(TaskAttribute.DESCRIPTION)) {
//            return FoglynAttribute.MYLYN_NEW_COMMENT.getKey();
//        }
        
        return super.mapToRepositoryKey(parent, key);
    }
}
