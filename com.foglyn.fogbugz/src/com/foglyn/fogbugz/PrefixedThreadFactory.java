/**
 * 
 */
package com.foglyn.fogbugz;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class PrefixedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public PrefixedThreadFactory(String prefix) {
        this.prefix = prefix;
    }
    
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName(prefix + "-" + counter.incrementAndGet());
        
        return t;
    }
}