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
