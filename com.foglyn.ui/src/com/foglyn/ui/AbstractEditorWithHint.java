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

package com.foglyn.ui;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * This is base class for simple editors based on Text field, which also display
 * hint, or decoration, when user edits it.
 */
abstract class AbstractEditorWithHint extends AbstractAttributeEditor {
    private static final int FOCUS_HINT_DELAY = 200;
    private static final int TYPING_HINT_DELAY = 400;

    protected ControlDecoration decoration = null;

    private Runnable enableHintRunnable;
    
    private boolean showingError;
    private String message;
    
    AbstractEditorWithHint(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
        
        this.enableHintRunnable = new EnableHint();
    }
    
    void decorateTextInput(Control control) {
        decoration = new ControlDecoration(control, SWT.RIGHT, control.getParent());
        decoration.setShowHover(true);
        decoration.setShowOnlyOnFocus(false);
        decoration.setMarginWidth(1);

        FieldDecoration fd = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
        decoration.setImage(fd.getImage());
        
        showingError = false;
    }

    void installListeners(final Text text) {
        text.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                text.getDisplay().timerExec(FOCUS_HINT_DELAY, enableHintRunnable);
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                text.getDisplay().timerExec(-1, enableHintRunnable);
                
                hideDecorationHint();
            }
        });
        
        text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character > 0) {
                    hideDecorationHint();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == SWT.ESC) {
                    text.getDisplay().timerExec(-1, enableHintRunnable);
                } else {
                    text.getDisplay().timerExec(TYPING_HINT_DELAY, enableHintRunnable);
                }
            }
        });
        
        text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                processCurrentValue();
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        
        if (decoration != null) {
            decoration.dispose();
            decoration = null;
        }
    }
    
    void setInfoMessage(String message) {
        setDecoration(false, message);
    }
    
    void setErrorMessage(String message) {
        setDecoration(true, message);
    }

    int getFieldDecorationWidth() {
        int w = 0;
        FieldDecoration fd = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
        
        if (fd != null) {
            w = Math.max(w, fd.getImage().getBounds().width);
        }

        fd = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        if (fd != null) {
            w = Math.max(w, fd.getImage().getBounds().width);
        }
        
        return w;
    }
    
    private void setDecoration(boolean error, String text) {
        FieldDecoration fd = null;
        
        if (this.showingError && !error) {
            fd = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
        } else if (!this.showingError && error) {
            fd = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        }

        if (fd != null) {
            decoration.setImage(fd.getImage());
        }

        this.message = text;
        this.showingError = error;
        
        decoration.setDescriptionText(message);
        // don't set hover text, as it would activate 'hint'
    }
    
    private void showDecorationHint() {
        decoration.showHoverText(message);
    }
    
    private void hideDecorationHint() {
        decoration.showHoverText(null);
    }
    
    private class EnableHint implements Runnable {
        public void run() {
            showDecorationHint();
        }
    }

    abstract void processCurrentValue();
}
