package esfingeplugin.util;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import esfingeplugin.ModificationTracker;

public class WorkbenchUtil {

	public static IProject getSelectedProject() throws NoProjectSelectedException {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IAdaptable) {
				IProject project = (IProject) ((IAdaptable) firstElement).getAdapter(IProject.class);
				return project;
			}
		}

		throw new NoProjectSelectedException();
	}

	public static boolean checkBuilderPresentInProject(IProject project) {
		// Get the description.
		IProjectDescription description;
		try {
			description = project.getDescription();
		} catch (CoreException e) {
			System.err.println(e);
			return false;
		}

		// Look for builder already associated.
		ICommand[] cmds = description.getBuildSpec();
		for (int j = 0; j < cmds.length; j++) {
			if (cmds[j].getBuilderName().equals(ModificationTracker.BUILDER_ID)) {
				return true;
			}
		}
		return false;
	}

	public static boolean checkBuilderPresentInSelectedProject() throws NoProjectSelectedException {
		return checkBuilderPresentInProject(getSelectedProject());
	}

}
