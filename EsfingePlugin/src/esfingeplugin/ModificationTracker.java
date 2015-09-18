package esfingeplugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import esfingeplugin.util.Markers;

public class ModificationTracker extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = Activator.PLUGIN_ID + ".analysemethods";

	MethodsAnalyser methodsAnalyzer = new MethodsAnalyser();
	LinkedList<IFile> changedSources = new LinkedList<IFile>();

	public ModificationTracker() {

	}

	@Override
	protected IProject[] build(final int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (lookForModifications(kind)) {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					IJavaProject proj = JavaCore.create(getProject());
					if (kind == FULL_BUILD) {
						methodsAnalyzer.analyzeMethods(proj);
					} else {
						methodsAnalyzer.analyzeMethods(proj, changedSources);
					}
				}
			}, monitor);
		}
		return null;
	}

	private boolean lookForModifications(int kind) {
		changedSources.clear();
		System.out.println("Looking for modifications on source code...");
		if (kind == FULL_BUILD) {
			System.out.println("Full build");
			return true;
		}

		IResourceDelta delta = getDelta(getProject());
		if (delta == null) {
			System.out.println("No files changed since last build");
			return false;
		}

		boolean thereIsChange = delta.getAffectedChildren().length > 0;
		try {
			for (IResourceDelta childDelta : delta.getAffectedChildren()) {
				scanChangeTree(childDelta);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		if (!thereIsChange) {
			System.out.println("No source code changes since last build");
		}

		return thereIsChange;
	}

	private void scanChangeTree(IResourceDelta resource) throws IOException, CoreException {
		IResourceDelta[] children = resource.getAffectedChildren();
		if (children.length == 0) {
			if (resource.getResource() instanceof IFile) {
				IFile file = (IFile) resource.getResource();
				if (resource.getKind() == IResourceDelta.ADDED || resource.getKind() == IResourceDelta.CHANGED || resource.getKind() == IResourceDelta.MOVED_TO) {
					if ("java".equals(file.getFileExtension())) {
						changedSources.add(file);
						System.out.println("  " + file.getName());
					}
				}
			}
		}

		for (int i = 0; i < children.length; i++) {
			scanChangeTree(children[i]);
		}
	}

	public static void addBuilderToProject(IProject project) {
		// Cannot modify closed projects.
		if (!project.isOpen()) {
			return;
		}

		// Get the description.
		IProjectDescription description;
		try {
			description = project.getDescription();
		} catch (CoreException e) {
			System.err.println(e);
			return;
		}

		// Look for builder already associated.
		ICommand[] cmds = description.getBuildSpec();
		for (int j = 0; j < cmds.length; j++) {
			if (cmds[j].getBuilderName().equals(BUILDER_ID)) {
				System.out.println(BUILDER_ID + " already defined.");
				return;
			}
		}

		// Associate builder with project.
		ICommand newCmd = description.newCommand();
		newCmd.setBuilderName(BUILDER_ID);
		List<ICommand> newCmds = new ArrayList<ICommand>();
		newCmds.addAll(Arrays.asList(cmds));
		newCmds.add(newCmd);
		description.setBuildSpec(newCmds.toArray(new ICommand[newCmds.size()]));
		try {
			project.setDescription(description, null);
		} catch (CoreException e) {
			System.err.println(e);
		}

		MethodsAnalyser ma = new MethodsAnalyser();
		try {
			ma.analyzeMethods(JavaCore.create(project));
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		System.out.println("Esfinge Builder Added");
	}

	public static void removeBuilderFromProject(IProject project) {

		Markers.deleteMarkers(project, Markers.INVALID_METHOD_NAME_MARKER_ID);
		Markers.deleteMarkers(project, Markers.INVALID_QUERYOBJECT_MARKER_ID);
		// Cannot modify closed projects.
		if (!project.isOpen()) {
			return;
		}

		// Get the description.
		IProjectDescription description;
		try {
			description = project.getDescription();
		} catch (CoreException e) {
			System.err.println(e);
			return;
		}

		// Look for builder.
		int index = -1;
		ICommand[] cmds = description.getBuildSpec();
		for (int j = 0; j < cmds.length; j++) {
			if (cmds[j].getBuilderName().equals(BUILDER_ID)) {
				index = j;
				break;
			}
		}
		if (index == -1) {
			return;
		}

		// Remove builder from project.
		List<ICommand> newCmds = new ArrayList<ICommand>();
		newCmds.addAll(Arrays.asList(cmds));
		newCmds.remove(index);
		description.setBuildSpec(newCmds.toArray(new ICommand[newCmds.size()]));
		try {
			project.setDescription(description, null);
		} catch (CoreException e) {
			System.err.println(e);
		}

		System.out.println("EsfingeError Builder Removed");
	}

}
