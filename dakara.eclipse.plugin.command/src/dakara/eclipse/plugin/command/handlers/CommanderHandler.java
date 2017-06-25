package dakara.eclipse.plugin.command.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings.HistoryEntry;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings.HistoryKey;
import dakara.eclipse.plugin.kavi.picklist.InputCommand;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;
import dakara.eclipse.plugin.stringscore.StringScore;
import dakara.eclipse.plugin.stringscore.StringScoreRanking;


@SuppressWarnings("restriction")
public class CommanderHandler extends AbstractHandler {
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		EclipseCommandProvider eclipseCommandProvider = new EclipseCommandProvider();
		List<QuickAccessElement> allEclipseCommands = eclipseCommandProvider.getAllCommands();
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		Function<HistoryKey, QuickAccessElement> historyItemResolver = historyKey -> eclipseCommandProvider.getCommand(historyKey.keys[0], historyKey.keys[1]);
		CommandDialogPersistedSettings<QuickAccessElement> historyStore = new CommandDialogPersistedSettings<>(100, item -> new HistoryKey(item.getProvider().getId(), item.getId()), historyItemResolver);
		historyStore.loadSettings();
		
		FieldResolver<QuickAccessElement> labelField = new FieldResolver<>("label",  item -> item.getLabel());
		FieldResolver<QuickAccessElement> providerField = new FieldResolver<>("provider",  item -> item.getProvider().getName());
		ListRankAndFilter<QuickAccessElement> listRankAndFilter = new ListRankAndFilter<>(
																	(filter, columnText) -> stringScore.scoreCombination(filter, columnText),
																	item -> item.getLabel());
		
		listRankAndFilter.addField(labelField.fieldId, labelField.fieldResolver);
		listRankAndFilter.addField(providerField.fieldId, providerField.fieldResolver);
		
		Consumer<QuickAccessElement> resolvedAction = (item) -> {
			window.getShell().getDisplay().asyncExec(item::execute);
			historyStore.addToHistory(item);
			historyStore.saveSettings();
		};
		
		final List<QuickAccessElement> historyItems = new ArrayList<>();
		for (HistoryEntry entry : historyStore.getHistory()) {
			historyItems.add((QuickAccessElement) entry.getHistoryItem());
		}
		
		Function<InputCommand, List<RankedItem<QuickAccessElement>>> listContentProvider = (inputCommand) -> {
			historyStore.setContentMode(inputCommand.contentMode);
			
			if (inputCommand.contentMode.equals("history")) {

				List<QuickAccessElement> uniqueHistoryItems = historyItems.stream().distinct().collect(Collectors.toList());
				List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, uniqueHistoryItems);
				if (inputCommand.getColumnFilter(0).length() == 0)
					return listRankAndFilter.moveItem(filteredList, historyItems.get(0), 0);
				else return filteredList;
			}
			
			List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, allEclipseCommands);
			if ((inputCommand.getColumnFilter(0).length() == 0) && (historyItems.size() > 0))
				return listRankAndFilter.moveItem(filteredList, historyItems.get(0), 0);
			else return filteredList;
		};
		
		KaviPickListDialog<QuickAccessElement> kaviPickList = new KaviPickListDialog<>();
		kaviPickList.addColumn(labelField.fieldId, labelField.fieldResolver).width(520);
		kaviPickList.addColumn(providerField.fieldId, providerField.fieldResolver).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		kaviPickList.setListContentProvider(listContentProvider);
		kaviPickList.setContentModes("normal", "history");
		kaviPickList.setContentMode(historyStore.getContentMode());
		kaviPickList.setResolvedAction(resolvedAction);
		kaviPickList.open();
		
		return null;
	}
}
