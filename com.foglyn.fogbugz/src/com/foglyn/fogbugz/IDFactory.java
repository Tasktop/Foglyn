package com.foglyn.fogbugz;

/**
 * Generic interface for creating ID objects. Useful when abstracting common code.
 */
public interface IDFactory<T extends ID> {
    T valueOf(String value);
}
