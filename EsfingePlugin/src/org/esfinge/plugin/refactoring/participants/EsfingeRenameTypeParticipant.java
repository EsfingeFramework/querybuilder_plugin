package org.esfinge.plugin.refactoring.participants;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.esfinge.plugin.refactoring.helpers.MapperHelper;
import org.esfinge.plugin.refactoring.helpers.QueryBuilderMethodHelper;

public class EsfingeRenameTypeParticipant extends EsfingeRenameParticipant {

	private IType type = null;
	protected Collection<IType> repositories = null;

	@Override
	protected boolean initialize(Object element) {
		type = (IType) element;
		return isApplicable();
	}

	private boolean isApplicable() {
		if (!getArguments().getUpdateReferences())
			return false;

		try {
			repositories = MapperHelper.getDirectRepositories(type);
		} catch (JavaModelException e) {
			return false;
		}

		if (repositories == null || repositories.size() == 0)
			return false;

		return true;
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) {
		RefactoringStatus status = new RefactoringStatus();

		for (IType repository : repositories)
			try {
				renameMethods(repository, pm);
			} catch (JavaModelException e) {
				status.merge(RefactoringStatus.createFatalErrorStatus("Error parsing repositories."));
			}

		return status;
	}

	private void renameMethods(IType repository, IProgressMonitor pm) throws JavaModelException {
		for (IMethod method : repository.getMethods()) {
			if (QueryBuilderMethodHelper.isValid(method.getElementName())) {
				String newMethodName = QueryBuilderMethodHelper.replaceType(method.getElementName(), getArguments().getNewName());
				renameElement(method, newMethodName, pm);
			}
		}
	}

	@Override
	public String getName() {
		return "Esfinge Rename Type Participant";
	}

}
