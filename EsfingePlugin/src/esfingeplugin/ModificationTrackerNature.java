package esfingeplugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import esfingeplugin.ModificationTracker;
import esfingeplugin.util.Markers;

public class ModificationTrackerNature implements IProjectNature {

	private IProject project;

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

	public void configure() throws CoreException {
		ModificationTracker.addBuilderToProject(project);
		new Job("Properties File Audit") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					project.build(ModificationTracker.FULL_BUILD, ModificationTracker.BUILDER_ID, null, monitor);
				} catch (CoreException e) {
					System.err.println(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	public void deconfigure() throws CoreException {
		ModificationTracker.removeBuilderFromProject(project);
		Markers.deleteMarkers(project, Markers.INVALID_METHOD_NAME_MARKER_ID);
	}

}
