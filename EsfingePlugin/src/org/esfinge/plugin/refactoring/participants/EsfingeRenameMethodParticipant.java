package org.esfinge.plugin.refactoring.participants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.esfinge.plugin.refactoring.helpers.GetterHelper;
import org.esfinge.plugin.refactoring.helpers.MapperHelper;
import org.esfinge.plugin.refactoring.helpers.QueryBuilderMethodHelper;
import org.esfinge.plugin.refactoring.helpers.QueryObjectHelper;

public class EsfingeRenameMethodParticipant extends EsfingeRenameParticipant {

	private IMethod method;
	private String oldFieldName, newFieldName;
	private Map<String, IType> prefixAndRepositories = null;

	@Override
	protected boolean initialize(Object element) {
		method = (IMethod) element;
		return isApplicable();
	}

	private boolean isApplicable() {
		if (!getArguments().getUpdateReferences())
			return false;

		oldFieldName = GetterHelper.getField(method.getElementName());
		if (oldFieldName == null)
			return false;

		IProgressMonitor pm = new NullProgressMonitor();
		try {
			prefixAndRepositories = MapperHelper.getPrefixesAndRepositories(method.getDeclaringType(), pm);
		} catch (JavaModelException e) {
			return false;
		}
		if (prefixAndRepositories.isEmpty())
			return false;

		return true;
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		RefactoringStatus status = new RefactoringStatus();

		newFieldName = GetterHelper.getField(getArguments().getNewName());
		if (newFieldName == null)
			status.merge(RefactoringStatus.createFatalErrorStatus("New name is not valid"));

		for (Entry<String, IType> prefixAndRepository : prefixAndRepositories.entrySet()) {
			String joinedPrefix = "";
			String uncapitalizedPrefix = "";
			String prefix = prefixAndRepository.getKey();
			if (!prefix.isEmpty()) {
				joinedPrefix = prefix.replaceAll("\\.", "");
				uncapitalizedPrefix = "";
				for (String part : prefix.split("\\."))
					uncapitalizedPrefix = uncapitalizedPrefix + QueryObjectHelper.uncapitalize(part) + ".";
			}

			IType repository = prefixAndRepository.getValue();

			try {
				renameQueryBuilderMethods(joinedPrefix, repository, pm);
			} catch (JavaModelException e) {
				status.merge(RefactoringStatus.createFatalErrorStatus("Error parsing repository"));
			}

			AstEdition astEdition = getAstEdition(repository.getCompilationUnit(), pm);
			renameDomainTerms(uncapitalizedPrefix, astEdition, pm);
			renameQueryObjects(joinedPrefix, astEdition, pm);
		}

		return status;
	}

	private void renameQueryBuilderMethods(String prefix, IType repository, IProgressMonitor pm) throws JavaModelException {
		for (IMethod method : repository.getMethods()) {
			String methodName = method.getElementName();
			if (QueryBuilderMethodHelper.hasField(methodName, prefix + oldFieldName)) {
				String newMethodName = QueryBuilderMethodHelper.replaceField(methodName, prefix + oldFieldName, prefix + newFieldName);
				renameElement(method, newMethodName, pm);
			}
		}
	}

	private void renameDomainTerms(final String prefix, AstEdition astEdition, IProgressMonitor pm) {
		final List<StringLiteral> properties = new ArrayList<StringLiteral>();
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(NormalAnnotation annotation) {
				if (annotation.getTypeName().toString().equals("Condition")) {
					for (Object value : annotation.values()) {
						if (value instanceof MemberValuePair) {
							MemberValuePair pair = (MemberValuePair) value;
							if (pair.getName().getIdentifier().equals("property")) {
								if (pair.getValue() instanceof StringLiteral) {
									StringLiteral stringLiteral = (StringLiteral) pair.getValue();
									if (stringLiteral.getLiteralValue().equals(prefix + QueryObjectHelper.uncapitalize(oldFieldName)))
										properties.add(stringLiteral);
								}
							}
						}
					}
				}
				return super.visit(annotation);
			}
		};
		astEdition.getCompilationUnit().accept(visitor);

		for (StringLiteral property : properties) {
			StringLiteral stringLiteral = astEdition.getAst().newStringLiteral();
			stringLiteral.setLiteralValue(prefix + QueryObjectHelper.uncapitalize(newFieldName));
			astEdition.getAstRewrite().replace(property, stringLiteral, null);
		}
	}

	private void renameQueryObjects(final String prefix, AstEdition astEdition, IProgressMonitor pm) {
		final List<IType> queryObjects = new ArrayList<IType>();
		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public boolean visit(MarkerAnnotation annotation) {
				if (annotation.getTypeName().toString().equals("QueryObject") && annotation.getParent() instanceof SingleVariableDeclaration) {
					queryObjects.add((IType) ((SingleVariableDeclaration) annotation.getParent()).getType().resolveBinding().getJavaElement());
				}
				return super.visit(annotation);
			}
		};
		astEdition.getCompilationUnit().accept(visitor);

		for (IType queryObject : queryObjects) {
			AstEdition queryObjectAstEdition = getAstEdition(queryObject.getCompilationUnit(), pm);

			final List<VariableDeclarationFragment> fieldsToBeRenamed = new ArrayList<VariableDeclarationFragment>();
			visitor = new ASTVisitor() {
				@Override
				public boolean visit(FieldDeclaration fieldDeclaration) {
					for (Object object : fieldDeclaration.fragments())
						if (object instanceof VariableDeclarationFragment) {
							VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
							if (QueryObjectHelper.Field.hasField(fragment.getName().toString(), prefix + oldFieldName))
								fieldsToBeRenamed.add(fragment);
						}
					return super.visit(fieldDeclaration);
				}
			};
			queryObjectAstEdition.getCompilationUnit().accept(visitor);

			for (VariableDeclarationFragment fragment : fieldsToBeRenamed) {
				String name = QueryObjectHelper.Field.replaceField(fragment.getName().toString(), prefix + oldFieldName, prefix + newFieldName);
				renameElement((IField) fragment.resolveBinding().getJavaElement(), name, pm);
			}

			final List<MethodDeclaration> queryObjectMethodsToBeRenamed = new ArrayList<MethodDeclaration>();
			visitor = new ASTVisitor() {
				@Override
				public boolean visit(MethodDeclaration methodDeclaration) {
					if (QueryObjectHelper.Method.hasField(methodDeclaration.getName().toString(), prefix + oldFieldName))
						queryObjectMethodsToBeRenamed.add(methodDeclaration);
					return super.visit(methodDeclaration);
				}
			};
			queryObjectAstEdition.getCompilationUnit().accept(visitor);

			for (MethodDeclaration methodDeclaration : queryObjectMethodsToBeRenamed) {
				String name = QueryObjectHelper.Method.replaceField(methodDeclaration.getName().toString(), prefix + oldFieldName, prefix + newFieldName);
				renameElement((IMethod) methodDeclaration.resolveBinding().getJavaElement(), name, pm);
			}
		}
	}

	@Override
	public String getName() {
		return "Rename Method Participant";
	}

}
