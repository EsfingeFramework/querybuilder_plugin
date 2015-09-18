package esfingeplugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import esfingeplugin.util.IntervalPrinter;
import esfingeplugin.util.ParserHelper;

public class Mapper {

	public final HashMap<String, Collection<IType>> mappedRepositories = new HashMap<String, Collection<IType>>();
	public final HashMap<IType, String> knowRepositories = new HashMap<IType, String>();
	public final HashMap<String, Collection<IType>> knownQueryObjects = new HashMap<String, Collection<IType>>();
	public final HashMap<String, String> entitiesBySimpleName = new HashMap<String, String>();

	IJavaProject proj;

	public static HashMap<String, Collection<IType>> getMappedRepositories(IJavaProject project) throws JavaModelException {
		Mapper mapper = new Mapper();
		mapper.mapAllRepositories(project);
		return mapper.mappedRepositories;
	}

	protected void mapAllRepositories(IJavaProject proj) throws JavaModelException {
		this.proj = proj;
		IntervalPrinter intervalPrinter = new IntervalPrinter();
		intervalPrinter.init();
		mappedRepositories.clear();
		knowRepositories.clear();
		entitiesBySimpleName.clear();
		knownQueryObjects.clear();

		try {
			IPackageFragment[] packages = proj.getPackageFragments();
			for (IPackageFragment mypackage : packages) {
				scanPackage(mypackage);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		intervalPrinter.print("map execution");
	}

	private void scanPackage(IPackageFragment mypackage) {
		try {
			ICompilationUnit[] cUnits = mypackage.getCompilationUnits();
			for (ICompilationUnit cUnit : cUnits) {
				scanCUnit(cUnit);
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void scanCUnit(ICompilationUnit cUnit) {
		try {
			IType[] types = cUnit.getTypes();
			for (IType type : types) {
				IAnnotation[] annotations = type.getAnnotations();
				for (IAnnotation an : annotations) {
					if ("QueryBuilder".equals(an.getElementName()) && ParserHelper.checkPackage("QueryBuilder", "org.esfinge.querybuilder.annotation", cUnit)) {
						mapTypeAsQueryBuilder(type);
						for (IMethod m : type.getMethods()) {
							ILocalVariable[] params = m.getParameters();
							if (params.length == 1) {
								for (IAnnotation paramAn : params[0].getAnnotations()) {
									if ("QueryObject".equals(paramAn.getElementName()) && ParserHelper.checkPackage("QueryObject", "org.esfinge.querybuilder.annotation", cUnit)) {
										String queryObjectName = ParserHelper.getFullName(params[0].getTypeSignature().substring(1).replace(";", ""), cUnit);
										Collection<IType> repos;
										if (knownQueryObjects.containsKey(queryObjectName)) {
											repos = knownQueryObjects.get(queryObjectName);
										} else {
											repos = new ArrayList<IType>();
											knownQueryObjects.put(queryObjectName, repos);
										}
										repos.add(type);
									}
								}
							}
						}
						break;
					}
				}
			}
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

	private void mapTypeAsQueryBuilder(IType type) throws JavaModelException {
		String entityFullName = ParserHelper.getEntityFullNameFromReturnTypeSignature(type.getMethods()[0].getReturnType(), type.getCompilationUnit());
		String entitySimpleName = ParserHelper.getSimpleName(entityFullName);
		if (entitiesBySimpleName.containsKey(entitySimpleName) && !entitiesBySimpleName.get(entitySimpleName).equals(entityFullName)) {
			return;
		}

		entitiesBySimpleName.put(entitySimpleName, entityFullName);
		Collection<IType> repositories;

		if (mappedRepositories.containsKey(entityFullName)) {
			repositories = mappedRepositories.get(entityFullName);
		} else {
			repositories = new HashSet<IType>();
			mappedRepositories.put(entityFullName, repositories);
		}

		repositories.add(type);
		knowRepositories.put(type, entityFullName);
	}

}
