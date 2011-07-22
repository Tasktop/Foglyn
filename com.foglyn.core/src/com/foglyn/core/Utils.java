package com.foglyn.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import com.foglyn.fogbugz.FogBugzException;
import com.foglyn.fogbugz.FogBugzCase.CaseID;
import com.foglyn.fogbugz.FogBugzPerson.PersonID;

public class Utils {
    public static Status toStatus(FogBugzException e) {
        return new Status(IStatus.ERROR, FoglynCorePlugin.PLUGIN_ID, e.getMessage(), e);
    }
    
    public static Status toErrorStatus(String message, Exception exc) {
        return new Status(IStatus.ERROR, FoglynCorePlugin.PLUGIN_ID, message, exc);
    }

    static TaskAttribute createAttribute(TaskData data, FoglynAttribute foglynAttribute) {
        return createAttribute(data.getRoot(), foglynAttribute);
    }

    static TaskAttribute createAttribute(TaskAttribute parentAttribute, FoglynAttribute foglynAttribute) {
        TaskAttribute attr = parentAttribute.createAttribute(foglynAttribute.getKey());
    
        TaskAttributeMetaData metaData = attr.getMetaData();
        metaData.setType(foglynAttribute.getType());
        metaData.setKind(foglynAttribute.getKind());
        metaData.setLabel(foglynAttribute.toString());
        metaData.setReadOnly(foglynAttribute.isReadOnly());
        return attr;
    }
    
    public static boolean isEmpty(String string) {
        return string == null || string.trim().length() == 0;
    }
    
    static boolean nonEmpty(String string) {
        return !isEmpty(string);
    }
    
    static String stripNewLines(String str) {
    	String result = str;
    	while (result.endsWith("\n") || result.endsWith("\r")) {
    		result = result.substring(0, result.length());
    	}
    	
    	return result;
    }
    
    /**
     * Parses numeric values from all supplied strings (usually coming from attribute.getValues()), and
     * returns list of found cases (possibly empty, but never null).
     */
    static List<CaseID> getCaseIDsFromString(List<String> subcases) {
        if (subcases == null || subcases.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<CaseID> result = null;
        
        for (String subcasesStr: subcases) {
            String[] caseIDs = subcasesStr.split(",\\s*");
            for (String caseID: caseIDs) {
                try {
                    CaseID cid = CaseID.valueOf(caseID);
                    if (result == null) {
                        result = new ArrayList<CaseID>();
                    }
                    
                    result.add(cid);
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }
        
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }
    
    public static CompletedCaseMode getCompletedCaseMode(TaskRepository repository) {
        if (repository == null) {
            return CompletedCaseMode.CLOSED;
        }

        String mode = repository.getProperty(FoglynConstants.REPOSITORY_COMPLETED_CASE_MODE);
        CompletedCaseMode ccm = CompletedCaseMode.getByID(mode);
        if (ccm != null) {
            return ccm;
        }
        
        return CompletedCaseMode.CLOSED;
    }

    public static void setCompletedCaseMode(TaskRepository repository, CompletedCaseMode mode) {
        if (repository == null) {
            return;
        }
        
        CompletedCaseMode m = mode;
        if (m == null) {
            m = CompletedCaseMode.CLOSED;
        }
        
        repository.setProperty(FoglynConstants.REPOSITORY_COMPLETED_CASE_MODE, m.getID());
    }
    
    public static boolean equal(PersonID currentUser, PersonID assignedTo) {
        if (currentUser == assignedTo) {
            // handles null == null case too
            return true;
        }
        
        if (currentUser != null) {
            return currentUser.equals(assignedTo);
        } else {
            return false;
        }
    }
    
    public static List<String> getTagsFromString(String tagsValue) {
        String[] tags = tagsValue.split("\\s+|,\\s*");
        
        List<String> processedTags = new ArrayList<String>();
        for (String tag: tags) {
            String t = tag.trim();
            if (t.length() > 0) {
                processedTags.add(t);
            }
        }
        
        return processedTags;
    }
    
    static String convertToTagsValue(List<String> tags) {
        StringBuilder result = new StringBuilder();
        
        String delim = "";
        for (String tag: tags) {
            result.append(delim);
            result.append(tag);
            
            delim = ", ";
        }
        
        return result.toString();
    }
    
    public static boolean isFogBugz7Repository(TaskRepository taskRepository, ITask task, TaskData taskData) {
        if (taskRepository != null) {
            return Boolean.parseBoolean(taskRepository.getProperty(FoglynConstants.REPOSITORY_IS_FOGBUGZ7_REPOSITORY));
        }

        if (task != null) {
            // set in FoglynTaskMapper.applyTo
            return Boolean.parseBoolean(task.getAttribute(FoglynConstants.REPOSITORY_IS_FOGBUGZ7_REPOSITORY));
        }

        if (taskData != null) {
            TaskAttribute a = taskData.getRoot().getAttribute(FoglynAttribute.REPOSITORY_IS_FOGBUGZ7_REPOSITORY.getKey());
            if (a != null) {
                return Boolean.parseBoolean(a.getValue());
            }
        }
        
        return false;
    }

    // TODO: convert to plain boolean... for now we want to support existing option in preferences page
    public static Boolean isWorkingOnSynchronizationEnabled(TaskRepository taskRepository) {
        if (taskRepository == null) {
            return null;
        }

        String prop = taskRepository.getProperty(FoglynConstants.REPOSITORY_SYNCHRONIZE_WORKING_ON);
        if (prop == null) {
            return null;
        }
        
        return Boolean.parseBoolean(prop);
    }
    
    public static String join(Iterable<?> objects, String beg, String delim, String end) {
        StringBuilder sb = new StringBuilder();
        
        String d = "";
        sb.append(beg);
        
        for (Object s: objects) {
            sb.append(d);
            sb.append(String.valueOf(s));
            
            d = delim;
        }
        
        sb.append(end);
        
        return sb.toString();
    }
    
    public static List<String> toStrings(Collection<?> objects) {
        List<String> result = new ArrayList<String>(objects.size());
        
        for (Object o: objects) {
            if (o == null) {
                result.add(null);
            } else {
                result.add(o.toString());
            }
        }
        
        return result;
    }

    public static void setWorkingOnSynchronization(TaskRepository repository, boolean workingOnSynchronizationEnabled) {
        if (repository == null) {
            return;
        }
        
        repository.setProperty(FoglynConstants.REPOSITORY_SYNCHRONIZE_WORKING_ON, Boolean.toString(workingOnSynchronizationEnabled));
    }
}
