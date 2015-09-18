package esfingeplugin.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import esfingeplugin.ModificationTracker;
import esfingeplugin.util.WorkbenchUtil;

public class AddBuilderHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			ModificationTracker.addBuilderToProject(WorkbenchUtil.getSelectedProject());
		} catch (Exception e) {
			System.err.println(e);
		}
		return null;
	}

}
