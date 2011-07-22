package com.foglyn.fogbugz;

import nu.xom.Element;

public interface Mapper<T> {
    public T mapElement(Element e) throws FogBugzException;
}
