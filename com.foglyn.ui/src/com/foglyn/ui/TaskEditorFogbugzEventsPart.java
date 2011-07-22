package com.foglyn.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.mylyn.internal.tasks.ui.editors.EditorUtil;
import org.eclipse.mylyn.tasks.core.IRepositoryPerson;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.foglyn.core.FoglynTaskCommentMapper;

/*
 * This code is based on TaskEditorCommentPart class, which isn't very friendly to subclasses, thus we copied most code here.
 */
@SuppressWarnings("restriction") // get rid of restriction warnings ... we use Mylyn internal API.
public class TaskEditorFogbugzEventsPart extends AbstractTaskEditorPart {
    private static final String REORDER_COMMENTS_PREF = "reorderComments";

    private static final String KEY_EDITOR = "viewer";

    private Section section;

    private List<CustomSection> commentComposites; // when non-null, main section was already expanded

    private List<TaskAttribute> comments;

    private boolean hasIncoming;
    
    private Font boldFont;

    private boolean reorderCommentsEnabled;
    
    public TaskEditorFogbugzEventsPart() {
        setPartName("Events");
        
        reorderCommentsEnabled = Boolean.parseBoolean(FoglynUIPlugin.loadOption(REORDER_COMMENTS_PREF, Boolean.FALSE.toString()));
    }

    @Override
	protected CustomSection createSection(Composite parent, FormToolkit toolkit, int sectionStyle) {
        CustomSection section = new CustomSection(parent, sectionStyle);
        section.setMenu(parent.getMenu());
        toolkit.adapt(section, true, true);
        if (section.getToggle() != null) {
            section.getToggle().setHoverDecorationColor(toolkit.getColors().getColor(IFormColors.TB_TOGGLE_HOVER));
            section.getToggle().setDecorationColor(toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
        }
        
        if (boldFont == null) {
            boldFont = getBoldFont(section.getDisplay(), parent.getFont());
        }
        section.setFont(boldFont);
        if ((sectionStyle & Section.TITLE_BAR) != 0
                || (sectionStyle & Section.SHORT_TITLE_BAR) != 0) {
            toolkit.getColors().initializeSectionToolBarColors();
            section.setTitleBarBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
            section.setTitleBarBorderColor(toolkit.getColors().getColor(IFormColors.TB_BORDER));
            section.setTitleBarForeground(toolkit.getColors().getColor(IFormColors.TB_TOGGLE));
        }
        section.setText(getPartName());
        return section;
    }
    
    private Font getBoldFont(Display display, Font normal) {
        FontData[] fontDatas = normal.getFontData();
        for (int i = 0; i < fontDatas.length; i++) {
            fontDatas[i].setStyle(fontDatas[i].getStyle() | SWT.BOLD);
        }
        return new Font(display, fontDatas);
    }
    
    private void expandComment(FormToolkit toolkit, Composite composite, final FoglynTaskCommentMapper taskComment, 
            boolean expanded) {
        // toolBarComposite.setVisible(expanded);
        if (expanded && composite.getData(KEY_EDITOR) == null) {
            // create viewer
            TaskAttribute textAttribute = getTaskData().getAttributeMapper().getAssoctiatedAttribute(taskComment.getCommentAttribute());
            
            AbstractAttributeEditor editor = createAttributeEditor(textAttribute);
            if (editor != null) {
                editor.setDecorationEnabled(false);
                editor.createControl(composite, toolkit);
                Control control = editor.getControl();
                if (control instanceof StyledText) {
                    StyledText st = (StyledText) control;
                    st.setWordWrap(true);
                    
                    // Don't use null caret, as it for some reason causes text not to be rendered. No idea why :-(
                    // st.setCaret(null);
                    Caret c = st.getCaret();
                    if (c != null) {
                        c.setVisible(false);
                    }
                }
                ((FoglynTaskEditorPage) getTaskEditorPage()).adaptControl(control);
                control.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseDown(MouseEvent e) {
                        getTaskEditorPage().selectionChanged(taskComment);
                    }
                });
                composite.setData(KEY_EDITOR, editor);

//                GridLayout gd = (GridLayout) composite.getLayout();
//                
//                int width = composite.getSize().x - gd.marginLeft - gd.marginRight - 1;
//                if (width <= 0) {
//                    width = EditorUtil.MAXIMUM_WIDTH;
//                }
//                
//                GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
//                    	.minSize(1, 1).hint(width, SWT.DEFAULT)
//	                    .grab(true, true).applyTo(control);
                
                getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);
                getTaskEditorPage().reflow();
            }
        } else if (!expanded && composite.getData(KEY_EDITOR) != null) {
            // dispose viewer
            AbstractAttributeEditor editor = (AbstractAttributeEditor) composite.getData(KEY_EDITOR);
            editor.getControl().setMenu(null);
            editor.getControl().dispose();
            composite.setData(KEY_EDITOR, null);
            getTaskEditorPage().reflow();
        }
        getTaskEditorPage().selectionChanged(taskComment);
    }

    //OK
    private void initialize() {
        comments = getTaskData().getAttributeMapper().getAttributesByType(getTaskData(), TaskAttribute.TYPE_COMMENT);
        if (comments.size() > 0) {
            for (TaskAttribute commentAttribute : comments) {
                if (getModel().hasIncomingChanges(commentAttribute)) {
                    hasIncoming = true;
                    break;
                }
            }
        }
    }

    //OK
    @Override
    public void createControl(Composite parent, final FormToolkit toolkit) {
        initialize();

        section = createSection(parent, toolkit, hasIncoming);
        section.setText(section.getText() + " (" + comments.size() + ")");

        if (comments.isEmpty()) {
            section.setEnabled(false);
        } else {
            if (hasIncoming) {
                expandSection(toolkit, section);
            } else {
                section.addExpansionListener(new ExpansionAdapter() {
                    @Override
                    public void expansionStateChanged(ExpansionEvent event) {
                        if (commentComposites == null) {
                            expandSection(toolkit, section);
                            getTaskEditorPage().reflow();
                        }
                    }
                });
            }
        }
        setSection(toolkit, section);
    }

    /**
     * Expands main "Comments" section: creates subsections for each comment/event
     */
    private void expandSection(final FormToolkit toolkit, final Section section) {
        final Composite composite = createComposite(toolkit, section);
        section.setClient(composite);
        composite.setLayout(new GridLayout(1, false));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

        commentComposites = new ArrayList<CustomSection>();
        
        for (final TaskAttribute commentAttribute : comments) {
            boolean hasIncomingChanges = getModel().hasIncomingChanges(commentAttribute);

            final FoglynTaskCommentMapper tcm = FoglynTaskCommentMapper.createFrom(commentAttribute);
            
            int style = ExpandableComposite.LEFT_TEXT_CLIENT_ALIGNMENT | ExpandableComposite.COMPACT;
            
            if (tcm.hasText() || tcm.hasChanges()) {
            	style |= ExpandableComposite.TREE_NODE;
            } else {
            	style |= CustomSection.INVISIBLE_TOGGLE;
            }
            
            if (hasIncomingChanges && (tcm.hasText() || tcm.hasChanges())) {
                style |= ExpandableComposite.EXPANDED;
            }
            
            final CustomSection commentComposite = createSection(composite, toolkit, style);
            
            commentComposite.setText(getEventDescription(tcm));
            commentComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            // these sections don't have title bars, so changing its color doesn't work...
            // but this color is still used inside ExpandableComposite code, but is null.
            // we will override setting code, and will return this color from getTitleBarForeground method
            commentComposite.setOverrideTitleBarForeground(true);
            commentComposite.setTitleBarForeground(toolkit.getColors().getColor(IFormColors.TITLE));
            commentComposite.clientVerticalSpacing = 0;
            if (hasIncomingChanges) {
                commentComposite.setBackground(getTaskEditorPage().getAttributeEditorToolkit().getColorIncoming());
            }

            commentComposites.add(commentComposite);

            if (tcm.hasChanges()) {
                String c = tcm.getChanges();
                while (c.endsWith("\n")) {
                    c = c.substring(0, c.length() - 1);
                }
                
                // escape '&' character (it would be underscored otherwise, since we use Label here)
                c = c.replace("&", "&&");
                
                // As we don't use client indent now, we will simulate it ourselves by using
                // composite with indented label inside

                Composite descComp = toolkit.createComposite(commentComposite);
                GridLayoutFactory.fillDefaults().spacing(0, 0).margins(commentComposite.getClientIndent(), 0).applyTo(descComp);

                Label descLabel = toolkit.createLabel(descComp, c);
                GridDataFactory.fillDefaults().grab(true, true).applyTo(descLabel);

                commentComposite.setDescriptionControl(descComp);
            }

            if (tcm.hasText()) {
                final Composite commentTextComposite = createComposite(toolkit, commentComposite);
                commentComposite.setClient(commentTextComposite);

                int focusWidth = FoglynUIPlugin.getDefault().getFocussWidth();
                
                // make enough space for focus border 
                int left = Math.max(focusWidth, commentComposite.getClientIndent());
                int right = focusWidth;
                int top = focusWidth + (tcm.hasText() && tcm.hasChanges() ? 3 : 0); // for spacing from 'changes'
                int bottom = focusWidth;
                
                commentTextComposite.setLayout(new FoglynFillWidthLayout(getLayoutAdvisor(getTaskEditorPage()), left, right, top, bottom));
                // commentTextComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                GridDataFactory.defaultsFor(commentTextComposite).align(SWT.FILL, SWT.CENTER).applyTo(commentTextComposite);
                
                commentComposite.addExpansionListener(new ExpansionAdapter() {
                    @Override
                    public void expansionStateChanged(ExpansionEvent event) {
                        expandComment(toolkit, commentTextComposite, tcm, event.getState());
                    }
                });

                if (hasIncomingChanges) {
                    expandComment(toolkit, commentTextComposite, tcm, true);
                }
            } else if (tcm.hasChanges()) {
                final Composite commentTextComposite = createComposite(toolkit, commentComposite);
                GridLayoutFactory.fillDefaults().applyTo(commentTextComposite);
                
                commentComposite.setClient(commentTextComposite);

                commentComposite.addExpansionListener(new ExpansionAdapter() {
                    @Override
                    public void expansionStateChanged(ExpansionEvent e) {
                        expandEmptyComment(toolkit, commentTextComposite, e.getState());
                    }
                });
                
                if (hasIncomingChanges) {
                    expandEmptyComment(toolkit, commentTextComposite, true);
                }
            }

            // for outline
            EditorUtil.setMarker(commentComposite, commentAttribute.getId());

            ((FoglynTaskEditorPage) getTaskEditorPage()).adaptControl(commentComposite);
        }
        
        if (reorderCommentsEnabled) {
            reorderComments();
        }
    }

    private Composite createComposite(FormToolkit toolkit, Section section) {
        Composite composite = new Composite(section, SWT.NONE | toolkit.getOrientation());
        composite.setBackground(toolkit.getColors().getBackground());
        composite.setMenu(section.getMenu());
        
        return composite;
    }

    private String getEventDescription(FoglynTaskCommentMapper taskComment) {
        StringBuilder sb = new StringBuilder();

        // Use evtDescription
        String evtDesc = taskComment.getEventDescription();
        if (evtDesc == null) {
            sb.append(taskComment.getVerb());
    
            IRepositoryPerson author = taskComment.getAuthor();
            if (author != null) {
                if (author.getName() != null) {
                    sb.append(" by ");
                    sb.append(author.getName());
                } else {
                    sb.append(" by ");
                    sb.append(author.getPersonId());
                }
            }
        } else {
            sb.append(evtDesc);
        }
        
        if (taskComment.getCreationDate() != null) {
            sb.append(", ");
            sb.append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(taskComment.getCreationDate()));
        }
        
        return sb.toString();
    }
    
    @Override
    protected void fillToolBar(ToolBarManager barManager) {
        if (comments.isEmpty()) {
            return;
        }

        Action reorderCommentsAction = new Action("") {
            @Override
            public void run() {
                reorderComments();
                
                reorderCommentsEnabled = !reorderCommentsEnabled;
                FoglynUIPlugin.saveOption(REORDER_COMMENTS_PREF, Boolean.valueOf(reorderCommentsEnabled).toString());
            }
        };

        reorderCommentsAction.setImageDescriptor(FoglynImages.UP_DOWN_SMALL);
        reorderCommentsAction.setToolTipText("Change Order of Events");
        barManager.add(reorderCommentsAction);
        
        Action collapseAllAction = new Action("") {
            @Override
            public void run() {
                hideOrExpandEvents(false);
            }
        };
        collapseAllAction.setImageDescriptor(FoglynImages.COLLAPSE_ALL_SMALL);
        collapseAllAction.setToolTipText("Collapse All Comments");
        barManager.add(collapseAllAction);

        Action expandAllAction = new Action("") {
            @Override
            public void run() {
                hideOrExpandEvents(true);
            }
        };
        expandAllAction.setImageDescriptor(FoglynImages.EXPAND_ALL_SMALL);
        expandAllAction.setToolTipText("Expand All Comments");
        barManager.add(expandAllAction);
    }

    private void hideOrExpandEvents (boolean expand) {
        if (commentComposites != null || expand) {
            Composite editorComposite = getTaskEditorPage().getEditorComposite();
            
            try {
                editorComposite.setCursor(editorComposite.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
                getTaskEditorPage().setReflow(false);

                if (expand && section != null) {
                    EditorUtil.toggleExpandableComposite(expand, section);
                }

                for (CustomSection composite : commentComposites) {
                    if (composite.isDisposed()) {
                        continue;
                    }
                    
                    composite.setReflowEnabled(false);

                    try {
                        if (composite.isExpanded() != expand) {
                            EditorUtil.toggleExpandableComposite(expand, composite);
                        }
                    } finally {
                        // make sure reflow is enabled after expanding
                        composite.setReflowEnabled(true);
                    }
                }
            } finally {
                getTaskEditorPage().setReflow(true);
                getTaskEditorPage().reflow();
                
                editorComposite.setCursor(null);
            }
        }
    }

    private void expandEmptyComment(final FormToolkit toolkit, final Composite composite, boolean expanded) {
        if (expanded && composite.getData(KEY_EDITOR) == null) {
            Label l = toolkit.createLabel(composite, "");
   
            GridDataFactory.fillDefaults().minSize(EditorUtil.MAXIMUM_WIDTH, 1).hint(EditorUtil.MAXIMUM_WIDTH,
                    1).applyTo(l);
            composite.setData(KEY_EDITOR, l);
        } else if (!expanded && composite.getData(KEY_EDITOR) != null) {
            // dispose viewer
            Control editor = (Control) composite.getData(KEY_EDITOR);
            editor.dispose();
            composite.setData(KEY_EDITOR, null);
        }
        
        getTaskEditorPage().reflow();
    }
    
    @Override
    public void dispose() {
        if (boldFont != null) {
            boldFont.dispose();
            boldFont = null;
        }
        super.dispose();
    }

    private void reorderComments() {
        if (commentComposites == null) {
            return;
        }
        
        Composite parent = null;
        
        List<CustomSection> newList = new ArrayList<CustomSection>();

        for (CustomSection c: commentComposites) {
            if (c.isDisposed()) {
                continue;
            }

            // we rely on the fact that parent uses GridLayout, which is sensitive to z-order reordering
            parent = c.getParent();
            c.moveAbove(null);
            newList.add(0, c);
        }
        
        commentComposites = newList;
        
        if (parent != null) {
            parent.layout(true);
        }
    }

    private static Composite getLayoutAdvisor(AbstractTaskEditorPage page) {
        Composite layoutAdvisor = page.getEditorComposite();
        do {
            layoutAdvisor = layoutAdvisor.getParent();
        } while (!(layoutAdvisor instanceof CTabFolder));
        return layoutAdvisor.getParent();
    }
}
