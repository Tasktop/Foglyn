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

package com.foglyn.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class CasePatterns {
    private final static List<Pattern> casePatterns = buildCasePatterns();
    
    private static List<Pattern> buildCasePatterns() {
        List<Pattern> patterns = new ArrayList<Pattern>();
        
        // 'Case XXX', 'case: XXXX', 'bugzid XXXX', 'BugzID: XXXX'
        patterns.add(Pattern.compile("(?:case:? ?)(\\d+)", Pattern.CASE_INSENSITIVE));
        patterns.add(Pattern.compile("(?:bugzid:? ?)(\\d+)", Pattern.CASE_INSENSITIVE));
        
        // This matches t:123 and ticket:123 
        patterns.add(Pattern.compile("(?:t|ticket):(\\d+)", Pattern.CASE_INSENSITIVE));
        
        return Collections.unmodifiableList(patterns);
    }

    /**
     * @return list of case patterns. Each pattern has two groups, group 2 returns numeric case ID.
     */
    public static List<Pattern> getPatterns() {
        return casePatterns;
    }
}
