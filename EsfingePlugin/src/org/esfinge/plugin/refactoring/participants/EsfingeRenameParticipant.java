package org.esfinge.plugin.refactoring.participants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.TextEdit;

public abstract class EsfingeRenameParticipant extends RenameParticipant {

	protected Map<ICompilationUnit, AstEdition> map = new HashMap<ICompilationUnit, AstEdition>();

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange changes = new CompositeChange("Rename method change");

		for (ICompilationUnit unit : map.keySet()) {
			TextEdit newEdit = map.get(unit).getAstRewrite().rewriteAST();
			TextChange existingChange = getTextChange(unit);

			if (existingChange == null) {
				CompilationUnitChange change = new CompilationUnitChange("change", unit);
				change.setEdit(newEdit);
				changes.add(change);
			} else {
				TextEdit existingEdit = existingChange.getEdit();
				if (existingEdit.covers(newEdit)) {
					mergeEdits(existingEdit, newEdit);
				} else {
					existingEdit.addChild(newEdit);
				}
			}
		}

		return changes;
	}

	protected AstEdition getAstEdition(ICompilationUnit iCompilationUnit, IProgressMonitor pm) {
		AstEdition astEdition;
		if (map.containsKey(iCompilationUnit)) {
			astEdition = map.get(iCompilationUnit);
		} else {
			astEdition = new AstEdition(iCompilationUnit, pm);
			map.put(iCompilationUnit, astEdition);
		}
		return astEdition;
	}

	protected void renameElement(final IMember element, String newName, IProgressMonitor pm) {
		for (ICompilationUnit iCompilationUnit : referencedCompilationUnits(element, pm)) {
			AstEdition astEdition = getAstEdition(iCompilationUnit, pm);

			final List<SimpleName> simpleNames = new ArrayList<SimpleName>();
			ASTVisitor visitor = new ASTVisitor() {
				@Override
				public boolean visit(SimpleName simpleName) {
					if (simpleName.resolveBinding().getJavaElement().equals(element))
						simpleNames.add(simpleName);
					return super.visit(simpleName);
				}
			};
			astEdition.getCompilationUnit().accept(visitor);
			SimpleName name = astEdition.getAst().newSimpleName(newName);
			for (SimpleName simpleName : simpleNames)
				astEdition.getAstRewrite().replace(simpleName, name, null);
		}
	}

	protected Set<ICompilationUnit> referencedCompilationUnits(IMember iMember, IProgressMonitor pm) {
		Set<ICompilationUnit> result = new HashSet<ICompilationUnit>();
		for (IMember reference : findElementReferences(iMember, pm))
			result.add(reference.getCompilationUnit());
		return result;
	}

	private List<IMember> findElementReferences(IMember iMember, IProgressMonitor pm) {
		final List<IMember> references = new ArrayList<IMember>();

		SearchRequestor requestor = new SearchRequestor() {
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				if (match.getAccuracy() == SearchMatch.A_ACCURATE)
					references.add((IMember) match.getElement());
			}
		};

		SearchEngine engine = new SearchEngine();

		IJavaSearchScope workspaceScope = SearchEngine.createWorkspaceScope();

		SearchPattern pattern = SearchPattern.createPattern(iMember, IJavaSearchConstants.ALL_OCCURRENCES, SearchPattern.R_EXACT_MATCH);

		SearchParticipant[] participant = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };

		try {
			engine.search(pattern, participant, workspaceScope, requestor, pm);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return references;
	}

	protected void mergeEdits(TextEdit sourceEdit, TextEdit newEdit) {
		for (TextEdit edit : newEdit.getChildren()) {
			if (affectEdit(sourceEdit, edit)) {
				System.out.println("Conflict");
			} else {
				if (newEdit.removeChild(edit)) {
					sourceEdit.addChild(edit);
				} else {
					// How did even you get here?
				}
			}
		}
	}

	private boolean affectEdit(TextEdit sourceEdit, TextEdit newEdit) {
		for (TextEdit edit : sourceEdit.getChildren()) {
			if (edit.covers(newEdit)) {
				return true;
			}
		}
		return false;
	}

	public class AstEdition {
		private CompilationUnit compilationUnit;
		private AST ast;
		private ASTRewrite astRewrite;

		public AstEdition(ICompilationUnit iCompilationUnit, IProgressMonitor pm) {
			compilationUnit = parse(iCompilationUnit, pm);
			ast = compilationUnit.getAST();
			astRewrite = ASTRewrite.create(ast);
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

		public CompilationUnit getCompilationUnit() {
			return compilationUnit;
		}

		public AST getAst() {
			return ast;
		}

		public ASTRewrite getAstRewrite() {
			return astRewrite;
		}
	}

}
