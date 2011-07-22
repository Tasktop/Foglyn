package com.foglyn.fogbugz;

/**
 * This interface has two purposes.
 * 
 * 1) It simplifies some coding in FogBugzClient by providing common type for generic methods.
 * 
 * 2) It allows ProGuard to keep package name unmodified by keeping this interface name untouched.
 */
public interface ID {
    // empty
}
