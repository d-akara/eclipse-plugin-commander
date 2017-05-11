package dakara.eclipse.plugin.command.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.StringScore;


@SuppressWarnings("restriction")
public class CommanderHandler extends AbstractHandler implements IStartup {

	@Override
	public void earlyStartup() {
		System.out.println("Dakara startup");
		
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		EclipseCommandProvider eclipseCommandProvider = new EclipseCommandProvider();
		KaviPickListDialog<QuickAccessElement> kaviPickList = new KaviPickListDialog<>();
		kaviPickList.addColumn(item -> item.getLabel()).width(420);
		kaviPickList.addColumn(item -> item.getProvider().getName()).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		kaviPickList.setListContentProvider(eclipseCommandProvider::getAllCommands);
		//kavaPickList.setListInitialContentProvider();
		kaviPickList.setListRankingStrategy((columnText, filter) -> StringScore.containsAnyOrderWords(filter, columnText));
		// set id function.  So that histories can be created
		// set default sorting
		// set list augmentation
		// auto select on exact match
		kaviPickList.setResolvedAction(item -> window.getShell().getDisplay().asyncExec(item::execute));
		kaviPickList.open();
		
		return null;
	}
}
