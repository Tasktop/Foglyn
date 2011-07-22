package com.foglyn.ui;

import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.NewTaskWizard;

public class FoglynNewTaskWizard extends NewTaskWizard {
    public FoglynNewTaskWizard(TaskRepository taskRepository, ITaskMapping taskSelection) {
        super(taskRepository, taskSelection);
    }
}
