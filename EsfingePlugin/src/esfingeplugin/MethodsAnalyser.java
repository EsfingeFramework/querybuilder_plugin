package esfingeplugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.esfinge.querybuilder.exception.QueryObjectException;
import org.esfinge.querybuilder.methodparser.DSLMethodParser;
import org.esfinge.querybuilder.methodparser.EntityClassProvider;
import org.esfinge.querybuilder.methodparser.MethodParser;
import org.esfinge.querybuilder.methodparser.QueryObjectMethodParser;

import esfingeplugin.util.IntervalPrinter;
import esfingeplugin.util.Markers;

public class MethodsAnalyser {

	private Mapper mapper = new Mapper();
	private ProjectClassLoader classLoader;
	private DSLMethodParser mpDSL = new DSLMethodParser();
	private QueryObjectMethodParser mpQO = new QueryObjectMethodParser();

	IJavaProject proj;

	public MethodsAnalyser() {
		EntityClassProvider entityClassProvider = new EntityClassProvider() {
			@Override
			public Class<?> getEntityClass(String name) {
				try {
					if (!mapper.entitiesBySimpleName.containsKey(name)) {
						return null;
					}
					return classLoader.loadClass(mapper.entitiesBySimpleName.get(name));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		mpDSL.setEntityClassProvider(entityClassProvider);
		mpQO.setEntityClassProvider(entityClassProvider);
	}

	public void analyzeMethods(IJavaProject proj) throws JavaModelException {
		IntervalPrinter intervalPrinter = new IntervalPrinter();
		initializeAnalysis(proj);

		intervalPrinter.init();
		for (IType repo : mapper.knowRepositories.keySet()) {
			analyseQueryBuilderMethods(repo);
		}
		intervalPrinter.print("analyse all repositories");
	}

	public void analyzeMethods(IJavaProject proj, Collection<IFile> changedSources) throws JavaModelException {
		IntervalPrinter intervalPrinter = new IntervalPrinter();
		initializeAnalysis(proj);

		intervalPrinter.init();
		Set<IType> reposToAnalyse = new HashSet<IType>();
		for (IFile file : changedSources) {
			extractReposToBeAnalysed(file, reposToAnalyse);
		}
		for (IType type : reposToAnalyse) {
			analyseQueryBuilderMethods(type);
		}

		intervalPrinter.print("analyse only modified files");
	}

	private void initializeAnalysis(IJavaProject proj) throws JavaModelException {
		this.proj = proj;
		mapper.mapAllRepositories(proj);

		classLoader = new ProjectClassLoader();
		classLoader.setJavaProject(proj);
	}

	private void extractReposToBeAnalysed(IFile file, Set<IType> reposToAnalyse) throws JavaModelException {
		ICompilationUnit cUnit = JavaCore.createCompilationUnitFrom(file);
		for (IJavaElement javaElem : cUnit.getChildren()) {
			if (javaElem instanceof IType) {
				IType type = (IType) javaElem;
				if (mapper.knowRepositories.containsKey(type)) {
					reposToAnalyse.add(type);
				} else if (mapper.mappedRepositories.containsKey(type.getFullyQualifiedName())) {
					for (IType repo : mapper.mappedRepositories.get(type.getFullyQualifiedName())) {
						reposToAnalyse.add(repo);
					}
				} else if (mapper.knownQueryObjects.containsKey(type.getFullyQualifiedName())) {
					try {
						Markers.deleteMarkers(type.getCompilationUnit().getCorrespondingResource(), Markers.INVALID_METHOD_NAME_MARKER_ID);
					} catch (JavaModelException e) {
						e.printStackTrace();
					}
					for (IType repo : mapper.knownQueryObjects.get(type.getFullyQualifiedName())) {
						reposToAnalyse.add(repo);
					}
				}
			}
		}
	}

	private String getTypeSignature(Class<?> type) {
		return Signature.createTypeSignature(type.getSimpleName(), false);
	}

	private void analyseQueryBuilderMethods(IType type) {
		try {
			Markers.deleteMarkers(type.getCompilationUnit().getCorrespondingResource(), Markers.INVALID_METHOD_NAME_MARKER_ID);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		try {
			Class<?> clazz = classLoader.loadClass(type.getFullyQualifiedName(), true);
			try {
				for (Method m : clazz.getDeclaredMethods()) {
					MethodParser mp = chooseMethodParser(m);
					mp.setInterface(clazz, classLoader);
					try {
						mp.parse(m);
					} catch (QueryObjectException e) {
						reportProblem(type, m, e.getMessage(), Markers.INVALID_METHOD_NAME_MARKER_ID);
						IType queryObjectType = proj.findType(e.getQueryObjectClass().toString().replace("class ", ""));
						try {
							Markers.deleteMarkers(queryObjectType.getCompilationUnit().getCorrespondingResource(), Markers.INVALID_QUERYOBJECT_MARKER_ID);
						} catch (JavaModelException e1) {
							e.printStackTrace();
						}
						reportProblem(queryObjectType, e.getFlawedMethod(), e.getMessage(), Markers.INVALID_QUERYOBJECT_MARKER_ID);
					} catch (Exception e) {
						reportProblem(type, m, e.getMessage(), Markers.INVALID_METHOD_NAME_MARKER_ID);
					}
				}
			} catch (NoClassDefFoundError e) {
				// e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void reportProblem(IType type, Method m, String msg, String markerId) {
		String[] parameterTypes = new String[m.getParameterTypes().length];
		for (int i = 0; i < parameterTypes.length; ++i) {
			parameterTypes[i] = getTypeSignature(m.getParameterTypes()[i]);
		}
		IMethod iMethod = type.getMethod(m.getName(), parameterTypes);
		try {
			Markers.reportProblem(msg, (IFile) type.getCompilationUnit().getCorrespondingResource(), iMethod.getNameRange().getOffset(), iMethod.getNameRange().getOffset() + iMethod.getNameRange().getLength(), true, markerId);
		} catch (JavaModelException e1) {
			e1.printStackTrace();
		}
	}

	private MethodParser chooseMethodParser(Method m) {
		if (m.getParameterTypes().length == 1) {
			for (Annotation an : m.getParameterAnnotations()[0]) {
				if ("org.esfinge.querybuilder.annotation.QueryObject".equals(an.annotationType().getName())) {
					return mpQO;
				}
			}
		}
		return mpDSL;
	}

}
