package com.foglyn.fogbugz;

class LongID implements ID {
    private final long id;
    
    protected LongID(long id) {
        this.id = id;
    }
    
    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
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
        
        LongID other = (LongID) obj;
        if (id != other.id)
            return false;
        
        return true;
    }

    @Override
    public final String toString() {
        return Long.toString(id);
    }
}
