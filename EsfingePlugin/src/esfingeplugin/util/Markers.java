package esfingeplugin.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import esfingeplugin.Activator;

public class Markers {

	public static final String INVALID_METHOD_NAME_MARKER_ID = Activator.PLUGIN_ID + ".invalidmethodname";
	public static final String INVALID_QUERYOBJECT_MARKER_ID = Activator.PLUGIN_ID + ".invalidqueryobject";

	public static boolean deleteMarkers(IResource project, String markerId) {
		try {
			project.deleteMarkers(markerId, false, IResource.DEPTH_INFINITE);
			return true;
		} catch (CoreException e) {
			System.err.println(e);
			return false;
		}
	}

	public static void reportProblem(String msg, IFile file, int charStart, int charEnd, boolean isError, String markerId) {
		try {
			IMarker marker = file.createMarker(Markers.INVALID_METHOD_NAME_MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, msg);
			marker.setAttribute(IMarker.CHAR_START, charStart);
			marker.setAttribute(IMarker.CHAR_END, charEnd);
			marker.setAttribute(IMarker.SEVERITY, isError ? IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING);
		} catch (CoreException e) {
			System.err.println(e);
			return;
		}
	}

}
