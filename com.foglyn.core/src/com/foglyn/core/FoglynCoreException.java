package com.foglyn.core;

import org.eclipse.core.runtime.CoreException;

import com.foglyn.fogbugz.FogBugzException;

/**
 * Simple exception wrapper for {@link FogBugzException}.
 */

// It is much better to use custom Exception class instead of creating
// CoreException in util method, because Findbugs can detect when we create
// exception and don't throw it ... but it doesn't detect this situation when
// exception is created in utility method.
public class FoglynCoreException extends CoreException {
	private static final long serialVersionUID = 8635660592541635514L;

	public FoglynCoreException(FogBugzException exception) {
        super(Utils.toStatus(exception));
    }
}
