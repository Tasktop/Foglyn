/*******************************************************************************
 * Copyright (c) 2008,2011 Peter Stibrany
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Peter Stibrany (pstibrany@gmail.com) - initial API and implementation
 *******************************************************************************/

package com.foglyn.fogbugz;

class StringID implements ID {
    private final String id;
    
    protected StringID(String id) {
        if (id == null) throw new IllegalArgumentException("id cannot be null");
        
        this.id = id;
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        
        // works OK with subclasses (only corresponding subclasses are equal)
        if (getClass() != obj.getClass())
            return false;
        
        StringID other = (StringID) obj;
        if (!id.equals(other.id))
            return false;
        
        return true;
    }

    @Override
    public final String toString() {
        return id;
    }
}
