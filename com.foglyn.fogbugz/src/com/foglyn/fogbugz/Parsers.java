package com.foglyn.fogbugz;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import nu.xom.Element;

import com.foglyn.fogbugz.FogBugzCase.CaseID;

/**
 * FogBugz parser helper methods.
 */
class Parsers {
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final CaseID CASE_ID_0 = CaseID.valueOf("0");

    static Set<CaseAction> parseActions(String operations) {
        Set<CaseAction> actions = EnumSet.noneOf(CaseAction.class);
        
        String[] commands = operations.split(",");
        for (String cmd: commands) {
            CaseAction action = CaseAction.getActionFromCommand(cmd);
            if (action != null) {
                actions.add(action);
            }
        }
        
        return actions;
    }

    static Date parseDate(String xpathValueOf) throws FogBugzException {
        if (xpathValueOf.trim().length() == 0) {
            return null;
        }
        
        try {
            return getDateFormatter().parse(xpathValueOf);
        } catch (ParseException e) {
            throw new FogBugzException("Illegal format of date value: " + xpathValueOf);
        }
    }

    static String formatDate(Date value) {
        return getDateFormatter().format(value);   
    }
    
    private static SimpleDateFormat getDateFormatter() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }
    
    static BigDecimal parseHours(String hours, boolean returnZero) {
        // special value which FogBugz uses to indicate no estimate/elapsed time
        if ("0".equals(hours)) {
            if (!returnZero) return null;
            
            return BigDecimal.ZERO;
        }
        
        BigDecimal result = new BigDecimal(hours);
        return result;
    }

    static List<CaseID> parseBugList(String ixRelatedBugs) {
        if (ixRelatedBugs == null || ixRelatedBugs.trim().length() == 0) {
            return Collections.emptyList();
        }
        
        List<CaseID> result = new ArrayList<CaseID>();
        
        String[] bugsIDs = ixRelatedBugs.trim().split(",");
        for (String bid: bugsIDs) {
            result.add(CaseID.valueOf(bid));
        }
        
        return result;
    }

    static CaseID parseCaseID(String caseNumber) {
        if (Utils.isEmpty(caseNumber)) return null;
        
        CaseID caseID = CaseID.valueOf(caseNumber);
        if (CASE_ID_0.equals(caseID)) return null;
        
        return caseID;
    }

    static List<String> parseTags(Element tagsElement) {
        if (tagsElement == null) {
            return Collections.emptyList();
        }

        List<Element> tags = XOMUtils.xpathElements(tagsElement, "tag");
        if (tags.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> result = new ArrayList<String>();
        for (Element t: tags) {
            result.add(t.getValue());
        }
        
        return result;
    }
}
