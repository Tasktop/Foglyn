package com.foglyn.fogbugz;

/**
 * Person is someone, who has assigned cases.
 * 
 * Person has full name, email address. Person can be virtual (usually mail
 * alias, or multiple email addresses), and deleted (case cannot be assigned to
 * deleted person).
 * 
 * Note: instances of this class are immutable.
 */
public final class FogBugzPerson implements HasID<FogBugzPerson.PersonID> {
    public final static class PersonID extends LongID {
        private PersonID(long caseID) {
            super(caseID);
        }

        public static PersonID valueOf(String ixPerson) {
            long id = Long.parseLong(ixPerson);
            
            if (id == 0) {
                return null;
            }
            return new PersonID(id);
        }
    }
    
    public static class PersonIDFactory implements IDFactory<PersonID> {
        public PersonID valueOf(String ixPerson) {
            return PersonID.valueOf(ixPerson);
        }
    }

    private final PersonID personID;

    private final String fullName;
    private final String email;
    private final boolean virtual;
    private final boolean inactive;

    FogBugzPerson(PersonID personID, String fullName, String email, boolean virtual, boolean inactive) {
        Utils.assertNotNullArg(personID, "personID");
        Utils.assertNotNullArg(fullName, "fullName");
        Utils.assertNotNullArg(email, "email");
        
        this.personID = personID;
        this.fullName = fullName;
        this.email = email;
        this.virtual = virtual;
        this.inactive = inactive;
    }
    
    public PersonID getID() {
        return personID;
    }
    
    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVirtual() {
        return virtual;
    }

    /**
     * @return true, if this person is inactive, i.e. deleted. Case cannot be
     *         assigned to inactive person.
     */
    public boolean isInactive() {
        return inactive;
    }

    private static final int MAX_EMAIL_LENGTH_IN_UI = 50;
    
    /**
     * Person name and email formatted for UI.
     */
    public String getFormattedPerson() {
        StringBuilder sb = new StringBuilder();
        sb.append(fullName);
        if (email != null && email.trim().length() > 0) {
            sb.append(" <");
            String te = email.trim();
            if (te.length() < MAX_EMAIL_LENGTH_IN_UI) {
                sb.append(te);
            } else {
                sb.append(te.substring(0, MAX_EMAIL_LENGTH_IN_UI - 3));
                sb.append("...");
            }
            sb.append(">");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "Person: " + fullName + " <" + email + "> (" + personID + ")";
    }
}
