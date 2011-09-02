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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

/**
 * This class implements buffering of section data.
 */
public class CustomSection extends Section {
	public static final int INVISIBLE_TOGGLE = 1 << 30;
	
    private Image cache;
    private Rectangle cacheBounds;
    
    private Color titleBarForegroundColor;
    private boolean overrideTitleBarForeground;
    
    private boolean reflowEnabled = true;

    public CustomSection(Composite parent, int style) {
        super(parent, computeStyle(style));
        
        if ((style & INVISIBLE_TOGGLE) != 0) {
        	if (toggle != null) {
        		toggle.dispose();
        	}
        	
        	toggle = new InvisibleToggle(this, SWT.NULL);
        	
        	// It's the toggle which gets focus, but focus border is displayed on text label.
            if ((getExpansionStyle() & FOCUS_TITLE)==0) {
                toggle.addFocusListener(new FocusListener() {
                    public void focusGained(FocusEvent e) {
                        textLabel.redraw();
                    }

                    public void focusLost(FocusEvent e) {
                        textLabel.redraw();
                    }
                });
            }
        }
    }
    
    private static int computeStyle(int style) {
    	if ((style & INVISIBLE_TOGGLE) != 0) {
    		style &= ~TWISTIE;
    		style |= TREE_NODE;
    		return style;
    	}
    	
    	return style;
    }

    @Override
    protected void onPaint(PaintEvent e) {
        Rectangle bounds = getClientArea();
        
        if (cacheBounds != null && cacheBounds.equals(bounds) && cache != null) {
            // paint from cache

            e.gc.drawImage(cache, 0, 0);
            return;
        }
        
        if (cache != null) {
            cache.dispose();
        }

        cache = new Image(getDisplay(), bounds.width, bounds.height);
        cacheBounds = bounds;
        
        GC newgc = new GC(cache);
        
        GC previous = e.gc;
        e.gc = newgc;
        try {
            super.onPaint(e);
        } finally {
            e.gc = previous;
            newgc.dispose();
        }

        e.gc.drawImage(cache, 0, 0);
    }
    
    @Override
    public void dispose() {
        if (cache != null) {
            cache.dispose();
        }
        
        super.dispose();
    }

    public ToggleHyperlink getToggle() {
        return toggle;
    }
    
    public int getClientIndent() {
        int gap = 4; // see ExpandableComposite.IGAP
        if (toggle != null) {
            Point size = toggle.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            gap += size.x;
        }
        
        return gap;
    }

    public void setOverrideTitleBarForeground(boolean overrideTitleBarForeground) {
        this.overrideTitleBarForeground = overrideTitleBarForeground;
    }

    @Override
    public void setTitleBarForeground(Color color) {
        super.setTitleBarForeground(color);
        
        titleBarForegroundColor = color;
    }
    
    @Override
    public Color getTitleBarForeground() {
        if (overrideTitleBarForeground) {
            return titleBarForegroundColor;
        }
        
        return super.getTitleBarForeground();
    }

    @Override
    protected void reflow() {
        if (reflowEnabled) {
            super.reflow();
        }
    }
    
    public void setReflowEnabled(boolean reflowEnabled) {
        this.reflowEnabled = reflowEnabled;
    }
}
