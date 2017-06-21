package dakara.eclipse.plugin.command.handlers;

import java.util.function.Function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings.HistoryKey;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettingsTest.TestItem;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.StringScore;
import dakara.eclipse.plugin.stringscore.StringScoreRanking;


@SuppressWarnings("restriction")
public class CommanderHandler extends AbstractHandler {
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		EclipseCommandProvider eclipseCommandProvider = new EclipseCommandProvider();
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		Function<HistoryKey, QuickAccessElement> historyItemResolver = historyKey -> eclipseCommandProvider.getCommand(historyKey.keys[0], historyKey.keys[1]);
		CommandDialogPersistedSettings historyStore = new CommandDialogPersistedSettings<QuickAccessElement>(10, item -> new HistoryKey(item.getProvider().getId(), item.getId()), historyItemResolver);
		
		KaviPickListDialog<QuickAccessElement> kaviPickList = new KaviPickListDialog<>();
		kaviPickList.addColumn(item -> item.getLabel()).width(520);
		kaviPickList.addColumn(item -> item.getProvider().getName()).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		kaviPickList.setListContentProvider(eclipseCommandProvider::getAllCommands);
		//kavaPickList.setListInitialContentProvider();
		kaviPickList.setListRankingStrategy((filter, columnText) -> stringScore.scoreCombination(filter, columnText));
		kaviPickList.setSortFieldResolver(item -> item.getLabel());
		kaviPickList.setHistoryProvider(historyStore::getHistory);
		// kaviPickList.setItemIdResolver(item -> item.getId() + ":" + item.getProvider().getId);
		// set list augmentation
		// auto select on exact match
		kaviPickList.setResolvedAction(item -> window.getShell().getDisplay().asyncExec(item::execute));
		kaviPickList.open();
		
		return null;
	}
}
