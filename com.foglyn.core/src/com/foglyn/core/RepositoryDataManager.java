package com.foglyn.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.TaskRepository;

import com.foglyn.fogbugz.ID;
import com.foglyn.fogbugz.IDFactory;
import com.foglyn.fogbugz.XOMUtils;
import com.foglyn.fogbugz.FogBugzArea.AreaIDFactory;
import com.foglyn.fogbugz.FogBugzCategory.CategoryIDFactory;
import com.foglyn.fogbugz.FogBugzFixFor.FixForIDFactory;
import com.foglyn.fogbugz.FogBugzPerson.PersonIDFactory;
import com.foglyn.fogbugz.FogBugzPriority.PriorityIDFactory;
import com.foglyn.fogbugz.FogBugzProject.ProjectIDFactory;

/**
 * This class manages various locally-saved data for repositories.
 * 
 * In some cases, it also does conversion from/to XML.
 */
class RepositoryDataManager {
    private final File pluginArea;
    
    RepositoryDataManager(File pluginArea) {
        this.pluginArea = pluginArea;
    }

    DefaultCaseData getDefaultCaseData(TaskRepository repository) {
        File file = getDefaultCaseDataFile(repository);
        
        if (!file.exists()) {
            // return empty data
            return new DefaultCaseData();
        }
        
        Builder builder = new Builder();
        Document doc = null;
        try {
            doc = builder.build(file);
        } catch (ParsingException e) {
            FoglynCorePlugin.getDefault().log(Status.WARNING, "Unable to parse default case data for repository " + repository + " from file " + file.getAbsolutePath(), e);
            return new DefaultCaseData();
        } catch (IOException e) {
            FoglynCorePlugin.getDefault().log(Status.WARNING, "Unable to parse default case data for repository " + repository + " from file " + file.getAbsolutePath(), e);
            return new DefaultCaseData();
        }
        
        return readDefaultCaseDataFromXML(doc);
    }

    void saveDefaultCaseData(TaskRepository repository, DefaultCaseData data) {
        File file = getDefaultCaseDataFile(repository);
        
        File parent = file.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                FoglynCorePlugin.getDefault().log(Status.WARNING, "Unable to create directory " + parent.getAbsolutePath());
                return;
            }
        }

        Document doc = convertDefaultCaseDataToXML(data);
        
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            Serializer serializer = new Serializer(fos);
            serializer.write(doc);
        } catch (IOException e) {
            FoglynCorePlugin.getDefault().log(Status.WARNING, "Unable to write default case data for repository " + repository + ", to file " + file.getAbsolutePath(), e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                FoglynCorePlugin.getDefault().log(Status.WARNING, "Unable to write default case data for repository " + repository + ", to file " + file.getAbsolutePath(), e);
            }
        }
    }
    
    private File getDefaultCaseDataFile(TaskRepository repository) {
        File dir = getRepositoryDirFile(repository);
        
        return new File(dir, "defaultCaseData.xml");
    }
    
    private String getRepositoryDirName(String repositoryURL) {
        byte[] bytes = null;
        try {
            bytes = repositoryURL.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Unsupported encoding: UTF-8???", e);
        }

        StringBuilder result = new StringBuilder();
        
        for (byte b: bytes) {
            char c = (char) b;
            if ((c >= '0' && c <= '9') || (c>='a' && c<='z') || (c>='A' && c<='Z') || (c == '.')) {
                result.append(c);
            } else if (c == '/') {
                result.append("_");
            } else {
                result.append(String.format("%%%02x", (int) c));
            }
        }
        
        return result.toString();
    }

    private File getRepositoryDirFile(TaskRepository repository) {
        String repodir = getRepositoryDirName(repository.getUrl());
        
        return new File(pluginArea, repodir);
    }
    
    private DefaultCaseData readDefaultCaseDataFromXML(Document doc) {
        Element element = doc.getRootElement();

        DefaultCaseData dcd = new DefaultCaseData();
        
        dcd.setDefaultProject(convertTo(new ProjectIDFactory(), element, "project/@projectID"));
        dcd.setDefaultArea(convertTo(new AreaIDFactory(), element, "area/@areaID"));
        dcd.setDefaultOwner(convertTo(new PersonIDFactory(), element, "owner/@personID"));
        dcd.setDefaultCategory(convertTo(new CategoryIDFactory(), element, "category/@categoryID"));
        dcd.setDefaultPriority(convertTo(new PriorityIDFactory(), element, "priority/@priorityID"));
        dcd.setDefaultFixFor(convertTo(new FixForIDFactory(), element, "fixFor/@fixForID"));

        return dcd;
    }
    
    private Document convertDefaultCaseDataToXML(DefaultCaseData caseData) {
        Element result = new Element("defaultCaseData");

        addElement(result, caseData.getDefaultProject(), "project", "projectID");
        addElement(result, caseData.getDefaultArea(), "area", "areaID");
        addElement(result, caseData.getDefaultOwner(), "owner", "personID");
        addElement(result, caseData.getDefaultCategory(), "category", "categoryID");
        addElement(result, caseData.getDefaultPriority(), "priority", "priorityID");
        addElement(result, caseData.getDefaultFixFor(), "fixFor", "fixForID");
        
        return new Document(result);
    }

    private <T extends ID> T convertTo(IDFactory<T> factory, Node node, String xpath) {
        String value = XOMUtils.xpathValueOf(node, xpath);
        if (value.trim().length() > 0) {
            return factory.valueOf(value);
        }
        
        return null;
    }
    
    private void addElement(Element result, ID project, String elementName, String attrName) {
        if (project != null) {
            Element projectElement = new Element(elementName);
            projectElement.addAttribute(new Attribute(attrName, project.toString()));
            result.appendChild(projectElement);
        }
    }
}
