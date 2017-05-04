package dakara.eclipse.plugin.command.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.StringScore;


@SuppressWarnings("restriction")
public class CommandHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
		EclipseCommandProvider eclipseCommandProvider = new EclipseCommandProvider();
		KaviPickListDialog<QuickAccessElement> kaviPickList = new KaviPickListDialog<>();
		kaviPickList.addColumn(item -> item.getLabel()).width(420);
		kaviPickList.addColumn(item -> item.getProvider().getName()).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		kaviPickList.setListContentProvider(filter -> eclipseCommandProvider.getAllCommands());
		kaviPickList.setListRankingStrategy((columnText, filter) -> StringScore.contains(filter, columnText));
		// set initial list
		// set default sorting
		// set list augmentation
		// auto select on exact match
		kaviPickList.setResolvedAction(item -> shell.getDisplay().asyncExec(item::execute));
		kaviPickList.open();
	}
}
