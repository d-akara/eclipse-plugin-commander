package dakara.eclipse.plugin.command.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.picklist.RapidInputPickList;


@SuppressWarnings("restriction")
public class CommandHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
		EclipseCommandProvider eclipseCommandProvider = new EclipseCommandProvider();
		RapidInputPickList<QuickAccessElement> rapidInputPickList = new RapidInputPickList<>();
		rapidInputPickList.addColumn(item -> item.getLabel()).width(420);
		rapidInputPickList.addColumn(item -> item.getProvider().getName()).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		rapidInputPickList.setListContentProvider(filter -> eclipseCommandProvider.getAllCommands());
		rapidInputPickList.setListRankingStrategy((columnText, filter) -> StringScore.contains(filter, columnText));
		rapidInputPickList.setResolvedAction(item -> shell.getDisplay().asyncExec(item::execute));
		rapidInputPickList.open();
	}
}
