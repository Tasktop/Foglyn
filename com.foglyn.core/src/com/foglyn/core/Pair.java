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

public final class Pair<U, V> {
    private final U first;
    private final V second;
    
    Pair(U first, V second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + ((first == null) ? 0 : first.hashCode());
        result = 31 * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        
        if (!(obj instanceof Pair<?, ?>)) {
            return false;
        }
        
        Pair<?, ?> other = (Pair<?, ?>) obj;
        if (first == null && other.first != null) {
            return false;
        } else if (first != null && !first.equals(other.first))
            return false;

        if (second == null && other.second != null) {
            return false;
        } else if (second != null && !second.equals(other.second))
            return false;
        
        return true;
    }
}
