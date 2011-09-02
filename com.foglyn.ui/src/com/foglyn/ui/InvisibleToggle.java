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

package com.foglyn.ui;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

/**
 * Invisible toggle has same size as TreeNode, but paints nothing.
 */
public class InvisibleToggle extends ToggleHyperlink {
	public InvisibleToggle(Composite parent, int style) {
		super(parent, style);
		
		innerWidth = 10;
		innerHeight = 10;
	}

	@Override
	protected void paint(PaintEvent e) {
		// do nothing... it's invisible :-)
	}

	@Override
	protected void paintHyperlink(GC gc) {
		// do nothing... it's invisible :-)
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		redraw();
	}
}
