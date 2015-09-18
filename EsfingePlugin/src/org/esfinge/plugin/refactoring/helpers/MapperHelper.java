package org.esfinge.plugin.refactoring.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;

import esfingeplugin.Mapper;

public class MapperHelper {

	private static Map<String, Collection<IType>> getMappedRepositories(IJavaProject project) throws JavaModelException {
		return Mapper.getMappedRepositories(project);
	}

	public static Collection<IType> getDirectRepositories(IType type) throws JavaModelException {
		return getMappedRepositories(type.getJavaProject()).get(type.getFullyQualifiedName());
	}

	public static Map<String, IType> getPrefixesAndRepositories(IType type, IProgressMonitor pm) throws JavaModelException {
		Map<String, IType> repositories = new HashMap<String, IType>();

		for (IType model : getModels(type.getJavaProject())) {
			for (String prefix : (new ModelTree(model, pm)).getPrefixes(type))
				for (IType repository : getDirectRepositories(model))
					repositories.put(prefix, repository);
		}

		return repositories;
	}

	private static List<IType> getModels(IJavaProject project) throws JavaModelException {
		List<IType> repositories = new ArrayList<IType>();
		for (String name : getMappedRepositories(project).keySet())
			repositories.add(project.findType(name));
		return repositories;
	}

	public static class ModelTree {
		private IType type;
		private String fieldName;

		private ModelTree parent;
		private Set<ModelTree> children;

		public ModelTree(final IType type, IProgressMonitor pm) throws JavaModelException {
			this(type, null, null, pm);
		}

		public List<String> getPrefixes(IType type) {
			List<String> result = new ArrayList<String>();
			for (ModelTree model : findModelTrees(type))
				result.add(getPrefix(model));
			return result;
		}

		private String getPrefix(ModelTree model) {
			List<String> antecedents = new ArrayList<String>();
			while (model.parent != null) {
				antecedents.add(model.fieldName);
				model = model.parent;
			}
			Collections.reverse(antecedents);

			String result = "";
			for (String antescendente : antecedents)
				result = result + "." + antescendente;

			if (result.isEmpty())
				return "";
			return result.substring(1);
		}

		private List<ModelTree> findModelTrees(IType searchedType) {
			List<ModelTree> result = new ArrayList<ModelTree>();
			if (type.equals(searchedType))
				result.add(this);
			for (ModelTree child : children)
				result.addAll(child.findModelTrees(searchedType));
			return result;
		}

		private ModelTree(final IType type, String fieldName, ModelTree parent, IProgressMonitor pm) throws JavaModelException {
			this.type = type;
			this.fieldName = fieldName;

			this.parent = parent;
			this.children = new HashSet<ModelTree>();

			for (Entry<IType, String> entry : getGetter(type, pm).entrySet())
				if (fromSource(entry.getKey()))
					addChild(entry.getKey(), entry.getValue(), pm);
		}

		private boolean addChild(IType type, String methodName, IProgressMonitor pm) throws JavaModelException {
			return children.add(new ModelTree(type, methodName, this, pm));
		}

		private CompilationUnit parse(ICompilationUnit source, IProgressMonitor pm) {
			ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(source);
			parser.setResolveBindings(true);

			CompilationUnit unit = (CompilationUnit) parser.createAST(pm);
			unit.recordModifications();
			return unit;
		}

		private Map<IType, String> getGetter(IType type, IProgressMonitor pm) {
			final Map<IType, String> childTypes = new HashMap<IType, String>();
			ASTVisitor visitor = new ASTVisitor() {
				@Override
				public boolean visit(MethodDeclaration methodDeclaration) {
					Type returnType = methodDeclaration.getReturnType2();
					String methodName = methodDeclaration.getName().toString();

					if (GetterHelper.isValid(methodName))
						if (!returnType.isPrimitiveType()) //avoiding NullPointerException when some entity attribute is primitive
							childTypes.put((IType) returnType.resolveBinding().getJavaElement(), GetterHelper.getField(methodName));

					return super.visit(methodDeclaration);
				}
			};
			parse(type.getCompilationUnit(), pm).accept(visitor);
			return childTypes;
		}

		private boolean fromSource(IType type) throws JavaModelException {
			for (IPackageFragment packageFragment : type.getJavaProject().getPackageFragments())
				if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE)
					for (ICompilationUnit compilationUnit : packageFragment.getCompilationUnits())
						if (compilationUnit.equals(type.getCompilationUnit()))
							return true;

			return false;
		}
	}

}
