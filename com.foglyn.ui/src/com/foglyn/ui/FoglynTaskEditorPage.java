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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.core.data.TaskDataModelEvent;
import org.eclipse.mylyn.tasks.core.data.TaskDataModelListener;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;
import org.eclipse.ui.forms.widgets.TreeNode;
import org.eclipse.ui.forms.widgets.Twistie;

import com.foglyn.core.FoglynAttribute;
import com.foglyn.core.FoglynConstants;
import com.foglyn.core.FoglynConstants.Dependency;
import com.foglyn.core.FoglynCorePlugin;

public class FoglynTaskEditorPage extends AbstractTaskEditorPage {
    private List<FoglynSingleSelectionWithDependenciesAttributeEditor> selectionEditors = new ArrayList<FoglynSingleSelectionWithDependenciesAttributeEditor>();

    private static final String FOGLYN_ADAPTED = "foglyn_adapted";
    private static final String FOGLYN_PAINT_LISTENER = "foglyn_paintListener";

    /**
     * "Master attributes" are attributes, which other attributes (editors) can depend on.
     */
    private Map<Dependency, TaskAttribute> masterAttributes = new EnumMap<Dependency, TaskAttribute>(Dependency.class);

    private AbstractAttributeEditor assignedUserEditor = null;

    private int focusEventID = 0;

    private ScrolledForm form;
    
    public FoglynTaskEditorPage(TaskEditor editor) {
        super(editor, FoglynCorePlugin.CONNECTOR_KIND);
        
        // Add support for global submit button
        setNeedsSubmit(true);
        setNeedsSubmitButton(true);
    }

    @Override
    protected TaskDataModel createModel(TaskEditorInput input)
            throws CoreException {
        TaskDataModel model = super.createModel(input);
        
        storeMasterValues(model);
        
        model.addModelListener(new TaskDataModelListener() {
            @Override
            public void attributeChanged(TaskDataModelEvent event) {
                TaskAttribute a = event.getTaskAttribute();
                String depKey = a.getMetaData().getValue(FoglynConstants.META_DEPENDENCY_KEY);
                if (depKey != null) {
                    fireValueChanged(FoglynConstants.Dependency.fromKey(depKey), a.getValue());
                }

                if (FoglynAttribute.MYLYN_OPERATION.getKey().equals(a.getId())) {
                    enablePeopleSection(a);
                }
            }
        });
        
        return model;
    }

    private void enablePeopleSection(TaskAttribute operationAttribute) {
        if (assignedUserEditor == null || assignedUserEditor.getControl() == null || assignedUserEditor.getControl().isDisposed()) {
            return;
        }
        
        boolean canReassign = false;
        boolean anyCanReassignMetadata = false;
        
        String value = operationAttribute.getValue();

        // go over all operations to find out if given operation allows reassign
        TaskData taskData = getModel().getTaskData();
        List<TaskAttribute> ops = taskData.getAttributeMapper().getAttributesByType(taskData, TaskAttribute.TYPE_OPERATION);
        for (TaskAttribute op: ops) {
            // even main operation attribute (which holds current value) is of TYPE_OPERATION
            if (operationAttribute == op) continue;

            String canReassignMetadata = op.getMetaData().getValue(FoglynConstants.META_OPERATION_CAN_REASSIGN);
            if (canReassignMetadata != null) {
                anyCanReassignMetadata = true;
            }
            
            if (value.equals(op.getValue())) {
                canReassign = Boolean.parseBoolean(canReassignMetadata);
            }
        }
        
        // support for old cases, without canReassignMetadata
        // TODO: remove check in later Foglyn version
        if (anyCanReassignMetadata) {
            assignedUserEditor.getControl().setEnabled(canReassign);
        }
    }
    
    private void storeMasterValues(TaskDataModel model) {
        Map<String, TaskAttribute> attrs = model.getTaskData().getRoot().getAttributes();
        for (Entry<String, TaskAttribute> e: attrs.entrySet()) {
            TaskAttribute a = e.getValue();
            String depKey = a.getMetaData().getValue(FoglynConstants.META_DEPENDENCY_KEY);
            if (depKey != null) {
                Object prev = masterAttributes.put(FoglynConstants.Dependency.fromKey(depKey), e.getValue());
                if (prev != null) {
                    throw new IllegalStateException("Multiple master values for " + depKey);
                }
            }
        }
    }
    
    private void fireValueChanged(Dependency metaAttr, String value) {
        FoglynSingleSelectionWithDependenciesAttributeEditor[] editors = selectionEditors.toArray(new FoglynSingleSelectionWithDependenciesAttributeEditor[selectionEditors.size()]);
        for (FoglynSingleSelectionWithDependenciesAttributeEditor e: editors) {
            if (e.isDisposed()) {
                selectionEditors.remove(e);
            } else {
                e.setMasterValue(metaAttr, value);
            }
        }
    }
    
    @Override
    protected AttributeEditorFactory createAttributeEditorFactory() {
        AttributeEditorFactory factory = new AttributeEditorFactory(getModel(), getTaskRepository(), getEditorSite()) {
            @Override
            public AbstractAttributeEditor createEditor(String type, TaskAttribute taskAttribute) {
                AbstractAttributeEditor result = null;
                
                if (TaskAttribute.TYPE_SINGLE_SELECT.equals(type)) {
                    // use custom editor, with standard combo box. CCombo used in mylyn has some troubles with keyboard shortcuts :(
                    result = new FoglynSingleSelectionAttributeEditor(getModel(), taskAttribute);
                } else if (FoglynConstants.TYPE_CUSTOM_SINGLE_SELECT_WITH_DEPENDANCIES.equals(type)) {
                    FoglynSingleSelectionWithDependenciesAttributeEditor e = new FoglynSingleSelectionWithDependenciesAttributeEditor(getModel(), taskAttribute);
                    selectionEditors.add(e);

                    String dependsOn = taskAttribute.getMetaData().getValue(FoglynConstants.META_DEPENDS_ON);
                    
                    // set dependencies
                    Set<Dependency> deps = FoglynConstants.parseDependsOn(dependsOn);

                    Map<Dependency, String> depValues = new EnumMap<Dependency, String>(Dependency.class);
                    for (Dependency d: deps) {
                        depValues.put(d, null);
                        TaskAttribute master = masterAttributes.get(d);
                        if (master != null) {
                            depValues.put(d, master.getValue());
                        }
                    }
                    
                    e.setDependencies(depValues);
                    
                    result = e;
                } else if (FoglynConstants.TYPE_DAYS_HOURS_MINUTES.equals(type)) {
                    result = new DayHoursMinutesEditor(getModel(), taskAttribute);
                } else if (FoglynConstants.TYPE_CUSTOM_DATETIME.equals(type)) {
                    result = new FoglynDueTimeEditor(getModel(), taskAttribute);
                } else if (FoglynConstants.TYPE_RELATED_CASES.equals(type)) {
                    result = new FoglynRelatedCasesEditor(getModel(), taskAttribute);
                } else if (TaskAttribute.TYPE_TASK_DEPENDENCY.equals(type)) {
                    result = super.createEditor(type, taskAttribute);
                    if (result != null && result.getLayoutHint() != null) {
                        result.setLayoutHint(null);
                    }
                }

                if (result == null) {
                    result = super.createEditor(type, taskAttribute);
                }
                
                // store some interesting editors into local fields
                if (FoglynAttribute.ASSIGNED_TO_PERSON_ID.getKey().equals(taskAttribute.getId())) {
                    assignedUserEditor = result;
                }
                
                return result;
            }
        };
        return factory;
    }
    
    @Override
    protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
        Set<TaskEditorPartDescriptor> descriptors = super.createPartDescriptors();

        // we copy descriptors into result -- we want our events to be first in comments section
        Set<TaskEditorPartDescriptor> result = new LinkedHashSet<TaskEditorPartDescriptor>();

        result.add(new TaskEditorPartDescriptor(ID_PART_COMMENTS) {
            @Override
            public AbstractTaskEditorPart createPart() {
                return new TaskEditorFogbugzEventsPart();
            }
        }.setPath(PATH_COMMENTS));

        // don't copy unnecessary parts
        for (TaskEditorPartDescriptor taskEditorPartDescriptor: descriptors) {
            if (taskEditorPartDescriptor.getId().equals(ID_PART_COMMENTS)) {
                continue;
            }
            
            if (taskEditorPartDescriptor.getId().equals(ID_PART_PLANNING)) {
                continue;
            }

            if (taskEditorPartDescriptor.getId().equals(ID_PART_DESCRIPTION)) {
                continue;
            }
            
            result.add(taskEditorPartDescriptor);
        }
        
        return result;
    }
    
    @Override
    protected void createFormContent(IManagedForm managedForm) {
        super.createFormContent(managedForm);
        
        form = managedForm.getForm();
        
        adaptControl(form.getBody());

        // Don't assume that task data is available ... it may not be
        if (getModel() != null) {
            TaskData taskData = getModel().getTaskData();
            TaskAttribute operationAttribute = taskData.getRoot().getAttribute(FoglynAttribute.MYLYN_OPERATION.getKey());
            if (operationAttribute != null) {
                enablePeopleSection(operationAttribute);
            }
        }
    }
    
    @Override
    public void showBusy(boolean busy) {
        super.showBusy(busy);

        // Don't assume that task data is available ... it may not be
        if (!busy && getModel() != null) {
            // we need to enable/disable "assigned to" in showBusy, because this method by default enables all controls on the page (!) 
            TaskAttribute operationAttribute = getModel().getTaskData().getRoot().getAttribute(FoglynAttribute.MYLYN_OPERATION.getKey());
            if (operationAttribute != null) {
                enablePeopleSection(operationAttribute);
            }
        }
    }

    // Allows tab/shift-tab inside styled text component, which otherwise disallows this.
    private TraverseListener styledTextTraversal = new TraverseListener() {
        public void keyTraversed(TraverseEvent e) {
            if (!(e.widget instanceof StyledText)) return;
            
            // Allow tab to work correctly. Why bothering with entering tab character, if most people don't use it anyway?
            // if (((StyledText) e.widget).getEditable() == true) return;
            
            if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                e.doit = true;
            }
        }
    };

    // Scrolls entire form up/down (pgup/pgdown) on arrow and paging keys
    private KeyListener scroller = new KeyAdapter() {
        @Override
		public void keyPressed(KeyEvent e) {
            if (e.widget instanceof StyledText) {
                StyledText styled = (StyledText) e.widget;
                if (styled.getEditable()) {
                    // don't scroll whole form when in editable Styled Text
                    return;
                }
            }
            
            if (e.widget instanceof Text) {
                Text t = (Text) e.widget;
                if ((t.getStyle() & SWT.READ_ONLY) == 0) {
                    // Don't scroll in editable text field
                    return;
                }
            }
            
            if ((e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_RIGHT) && (e.widget instanceof Twistie || e.widget instanceof TreeNode)) {
                return;
            }
            
            e.doit = !scroll(true, Scroll.getScrollFromKey(e.keyCode, e.stateMask));
        }
    };
    
    // Disable scrolling by up/down arrows (some controls do this)
    private TraverseListener disableTraverseAndScroll = new TraverseListener() {
        public void keyTraversed(TraverseEvent e) {
            if (e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.ARROW_UP) {
                scroll(true, Scroll.getScrollFromKey(e.keyCode, e.stateMask));
                
                e.detail = SWT.TRAVERSE_NONE;
                e.doit = true;
            }
        }
    };

    /**
     * This paint listener draws focus border around focused control inside Composite.
     */
    private PaintListener drawFocusBorderListener = new PaintListener() {
        public void paintControl(PaintEvent e) {
            if (!(e.widget instanceof Composite)) {
                return;
            }
            
            Composite comp = (Composite) e.widget;
            
            int focusWidth = FoglynUIPlugin.getDefault().getFocussWidth();
            
            Control[] children = comp.getChildren();
            for (Control c: children) {
                if (c.isFocusControl()) {
                    Rectangle r = c.getBounds();
                    GC gc = e.gc;
                    
                    // don't paint over control itself, because it will be repainted
                    gc.drawFocus(r.x-focusWidth, r.y-focusWidth, r.width+2*focusWidth, r.height+2*focusWidth);
                }
            }
        }
    };
    
    private FocusListener redrawFocusListener = new FocusListener() {
        public void focusGained(FocusEvent e) {
            redraw((Control) e.widget);
        }

        public void focusLost(FocusEvent e) {
            redraw((Control) e.widget);
        }

        private void redraw(Control control) {
            if (control instanceof StyledText) {
                // ping parent to draw focus border around StyledText (not inside).
                control.getParent().redraw();
            } else {
                control.redraw();
            }
        }
    };
    
    private enum Scroll {
        NONE, UP, DOWN, LEFT, RIGHT, PAGE_UP, PAGE_DOWN, PAGE_LEFT, PAGE_RIGHT, TOP, BOTTOM;
        
        static Scroll getScrollFromKey(int keyCode, int stateMask) {
            if (keyCode == SWT.PAGE_UP) return PAGE_UP;
            if (keyCode == SWT.PAGE_DOWN) return PAGE_DOWN;
            if (keyCode == SWT.ARROW_UP) return UP;
            if (keyCode == SWT.ARROW_DOWN) return DOWN;
            if (keyCode == SWT.ARROW_LEFT && ((stateMask & SWT.CONTROL) != 0)) return PAGE_LEFT;
            if (keyCode == SWT.ARROW_LEFT) return LEFT;
            if (keyCode == SWT.ARROW_RIGHT && ((stateMask & SWT.CONTROL) !=0)) return PAGE_RIGHT;
            if (keyCode == SWT.ARROW_RIGHT) return RIGHT;
            if (keyCode == SWT.HOME) return TOP;
            if (keyCode == SWT.END) return BOTTOM;
            return NONE;
        }
    }

    boolean scroll(boolean lineScrollingEnabled, Scroll direction) {
        if (!lineScrollingEnabled && (direction == Scroll.UP || direction == Scroll.DOWN)) {
            return false;
        }
        
        ScrolledForm form = getManagedForm().getForm();

        Point p = form.getOrigin();
        switch (direction) {
        case UP:        p.y = p.y - form.getVerticalBar().getIncrement(); break;
        case DOWN:      p.y = p.y + form.getVerticalBar().getIncrement(); break;
        case PAGE_UP:   p.y = p.y - form.getVerticalBar().getPageIncrement(); break;
        case PAGE_DOWN: p.y = p.y + form.getVerticalBar().getPageIncrement(); break;
        case LEFT:      p.x = p.x - form.getHorizontalBar().getIncrement(); break;
        case RIGHT:     p.x = p.x + form.getHorizontalBar().getIncrement(); break; 
        case PAGE_LEFT:      p.x = p.x - form.getHorizontalBar().getPageIncrement(); break;
        case PAGE_RIGHT:     p.x = p.x + form.getHorizontalBar().getPageIncrement(); break; 
        case TOP: p.y = 0; p.x = 0; break;
        case BOTTOM: p.y = Integer.MAX_VALUE; p.x = 0; break;
        case NONE: return false; // don't scroll
        }

        if (p.y < 0) p.y = 0;
        if (p.x < 0) p.x = 0;
        form.setOrigin(p);
        return true;
    }

    /* Move viewport so that given control is visible */
    void scrollTo(Control control) {
        Integer feID = (Integer) control.getData("focusEventID");
        if (feID != null && feID.intValue() == focusEventID) return;
        
        focusEventID ++;
        
        ScrolledForm form = getManagedForm().getForm();

        // bounds of control, relative to the visible upper-left corner of form
        Rectangle itemRect = form.getDisplay().map(control.getParent(), form, control.getBounds());
        
        // visible part of form
        Rectangle area = form.getClientArea();
        
        // visible upper-left corner of form, absolute coordinates
        Point origin = form.getOrigin();
        if (itemRect.x < 0 || itemRect.x > area.width) {
            int diff = Math.min(itemRect.width - area.width, 0);
            origin.x = Math.max(0, origin.x + itemRect.x + diff);
        }
        if (itemRect.y < 0 && itemRect.y + itemRect.height < 0) {
            origin.y = Math.max(0, origin.y + itemRect.y);
        } else if (itemRect.y > area.height) {
            int diff = Math.min(itemRect.height - area.height, 0);
            origin.y = Math.max(0, origin.y + itemRect.y + diff);
        }

        form.setOrigin(origin);
        
        control.setData("focusEventID", focusEventID);
    }
    
    /*
     *  We modify each control in case editor in following way:
     *    * we keep control with focus in visible part
     *    * traverse by up/down arrows is disabled on certain parts of case editor
     *    * non-editable styled text editor doesn't use up/down arrows anymore, but scrolls whole case editor instead
     *    * we attach key listener to other controls, which moves entire editor up/down
     */ 
    void adaptControl(Control control) {
        if (control instanceof Composite) {
            Control[] children = ((Composite) control).getChildren();
            for (Control c: children) {
                adaptControl(c);
            }
        }
        
        // Don't "adapt" twice (but allow adaption of new children in composites)
        if (Boolean.TRUE.equals(control.getData(FOGLYN_ADAPTED))) {
            return;
        }
        
        control.setData(FOGLYN_ADAPTED, Boolean.TRUE);

        control.addFocusListener(new FocusAdapter() {
            @Override
			public void focusGained(FocusEvent e) {
                if (!(e.widget instanceof Control)) {
                    return;
                }
                scrollTo((Control) e.widget);
            }
        });
        
        if (control instanceof ToggleHyperlink || control instanceof ToolBar) {
            control.addTraverseListener(disableTraverseAndScroll);
        }
        
        if (control.getClass().getName().endsWith("LayoutComposite") || 
                control instanceof Combo || 
                control instanceof CCombo || 
                control instanceof org.eclipse.swt.widgets.List) {
            return;
        }
        
        // don't add key listener to composite, or it will take focus
        if (control.getClass() != Composite.class) {
            control.addKeyListener(scroller);
        }

        if (control instanceof StyledText) {
            StyledText styledText = (StyledText) control;
            
            if ((styledText.getStyle() & SWT.MULTI) != 0 && !styledText.getEditable()) {
                // disable up/down arrows on read only styled text
                styledText.setKeyBinding(SWT.ARROW_UP, SWT.NONE);
                styledText.setKeyBinding(SWT.ARROW_DOWN, SWT.NONE);
                styledText.setKeyBinding(SWT.ARROW_LEFT, SWT.NONE);
                styledText.setKeyBinding(SWT.ARROW_RIGHT, SWT.NONE);
                styledText.setKeyBinding(SWT.PAGE_UP, SWT.NONE);
                styledText.setKeyBinding(SWT.PAGE_DOWN, SWT.NONE);
                styledText.setKeyBinding(SWT.HOME, SWT.NONE);
                styledText.setKeyBinding(SWT.END, SWT.NONE);

                Composite p = control.getParent();
                
                // don't add paint listener twice, or focus border will be painted incorrectly (twice or more times)
                if (!Boolean.TRUE.equals(p.getData(FOGLYN_PAINT_LISTENER))) {
                    p.addPaintListener(drawFocusBorderListener);
                    
                    p.setData(FOGLYN_PAINT_LISTENER, Boolean.TRUE);
                }
                
                control.addFocusListener(redrawFocusListener);
            }
            
            control.addTraverseListener(styledTextTraversal);
        }
    }
}
