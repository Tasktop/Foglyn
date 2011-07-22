package com.foglyn.helpers;

import com.foglyn.fogbugz.FogBugzPerson;

public class PersonLabelProvider extends TypedLabelProvider<FogBugzPerson> {
    public PersonLabelProvider() {
        super(FogBugzPerson.class);
    }

    @Override
    protected String getTextForElement(FogBugzPerson element) {
        return element.getFormattedPerson();
    }
}
