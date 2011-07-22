package com.foglyn.fogbugz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class Logging {
    private final static String LOGGER_PREFIX = "foglyn.fogbugz.";
    
    static Log getLogger(String name) {
        return LogFactory.getLog(LOGGER_PREFIX + name);
    }
}
