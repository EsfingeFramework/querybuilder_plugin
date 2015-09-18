package esfingeplugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class ProjectClassLoader extends ClassLoader {

	private IJavaProject project;

	private final HashMap<String, Class<?>> knowClasses = new HashMap<String, Class<?>>();

	public ProjectClassLoader() {
	}

	public void setJavaProject(IJavaProject project) {
		this.project = project;
	}

	private byte[] getClassImplFromDataBase(String classFullName) {
		try {
			IType type = project.findType(classFullName);
			if (type == null) return null;
			if (type.getClassFile() != null) {
				return type.getClassFile().getBytes();
			}
			IPath path = project.getOutputLocation().removeFirstSegments(1).append(classFullName.replace('.', '/') + ".class");
			InputStream in = ((IFile) project.getProject().findMember(path)).getContents();
			byte[] result = new byte[in.available()];
			in.read(result);
			return result;
		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		return (loadClass(className, true));
	}

	@Override
	public synchronized Class<?> loadClass(String classFullName, boolean resolveIt) throws ClassNotFoundException {
		Class<?> result;
		byte classData[];

		try {
			result = super.findSystemClass(classFullName);
			return result;
		} catch (ClassNotFoundException e) {

		}

		if (knowClasses.containsKey(classFullName)) {
			return knowClasses.get(classFullName);
		}

		classData = getClassImplFromDataBase(classFullName);
		if (classData == null) {
			throw new ClassNotFoundException();
		}

		result = defineClass(classFullName, classData, 0, classData.length);
		if (result == null) {
			throw new ClassFormatError();
		}

		knowClasses.put(classFullName, result);

		if (resolveIt) {
			resolveClass(result);
		}

		return result;
	}

}
