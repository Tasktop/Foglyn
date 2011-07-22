package com.foglyn.ui;

import net.miginfocom.swt.MigLayout;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class FoglynQueryTypeWizardPage extends WizardPage {

	private Button buttonSearch;

	private Button buttonFilter;

	private Composite composite;

	private final FoglynAdvancedSearchPage searchPage;

	private final FoglynFilterQueryPage filterPage;

	public FoglynQueryTypeWizardPage(TaskRepository repository) {
		super("Choose Type of Query");
		
		setTitle("Choose Type of Query");
		setDescription("Select type of query from available types");
		setImageDescriptor(FoglynImages.FOGBUGZ_REPOSITORY);
		
		filterPage = new FoglynFilterQueryPage(repository);
		searchPage = new FoglynAdvancedSearchPage(repository);
	}

	public void createControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(composite);
		
		composite.setLayout(new MigLayout());
		
		buttonFilter = new Button(composite, SWT.RADIO);
		buttonFilter.setText("Query based on your saved FogBugz filter");
		buttonFilter.setSelection(true);
		buttonFilter.setLayoutData("span");

		buttonSearch = new Button(composite, SWT.RADIO);
		buttonSearch.setText("Query based on search options");
        buttonSearch.setLayoutData("span");

		setPageComplete(true);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}

	@Override
	public IWizardPage getNextPage() {
		if (buttonFilter.getSelection()) {
			filterPage.setWizard(this.getWizard());
			return filterPage;
		}
		
		searchPage.setWizard(this.getWizard());
		return searchPage;
	}

}
