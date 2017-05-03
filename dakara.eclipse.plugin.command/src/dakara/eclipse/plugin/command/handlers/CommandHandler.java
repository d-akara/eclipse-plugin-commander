package dakara.eclipse.plugin.command.handlers;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.SWT;
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
		rapidInputPickList.addColumn(item -> item.getLabel()).setWidth(300);
		rapidInputPickList.addColumn(item -> item.getProvider().getName()).setWidth(200).setAlignment(SWT.RIGHT);
		rapidInputPickList.setListContentProvider(filter -> eclipseCommandProvider.getAllCommands());
		rapidInputPickList.setListRankingStrategy((item, filter) -> StringScore.contains(filter, item.getLabel()));
		rapidInputPickList.setResolvedAction(item -> shell.getDisplay().asyncExec(item::execute));
		rapidInputPickList.open();
	}
}
