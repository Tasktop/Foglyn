package com.foglyn.fogbugz;

/**
 * Marks entity which has ID, and can return it.
 * 
 * Useful for making code more generic.
 * 
 * @param <K> exact ID type
 */
public interface HasID<K extends ID> {
    public K getID();
}
