package dakara.eclipse.plugin.command.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
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
	private EclipseCommandProvider eclipseCommandProvider;
	private KaviPickListDialog<QuickAccessElement> kaviPickList;

	/* TODO's
	 * - refactor out most of the setup code here
	 * - research improving launch speed. reuse dialog.  https://www.eclipse.org/forums/index.php/t/197640/
	 * - allow other commands to reuse dialog to show other lists for faster speed
	 * - need ability to issue internal commands on lists
	 *   - for example, need command to remove an item from history
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (kaviPickList == null) {
			initialize(HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell().getDisplay());
		} else {
			eclipseCommandProvider.initializeWithCurrentContext();
			kaviPickList.show();
		}
		return null;
	}
	
	public void initialize(Display display) throws ExecutionException {
		FieldResolver<QuickAccessElement> labelField = labelFieldResolver();
		FieldResolver<QuickAccessElement> providerField = providerFieldResolver();
		ListRankAndFilter<QuickAccessElement> listRankAndFilter = listRankAndFilter(labelField, providerField);
		
		eclipseCommandProvider = new EclipseCommandProvider();
		CommandDialogPersistedSettings<QuickAccessElement> historyStore = createSettingsStore(display, eclipseCommandProvider);
		Function<InputCommand, List<RankedItem<QuickAccessElement>>> listContentProvider = listContentProvider(listRankAndFilter, historyStore, eclipseCommandProvider);
		
		Consumer<QuickAccessElement> resolvedAction = resolvedAction(display, historyStore);
		
		kaviPickList = new KaviPickListDialog<>();
		kaviPickList.addColumn(labelField.fieldId, labelField.fieldResolver).width(520);
		kaviPickList.addColumn(providerField.fieldId, providerField.fieldResolver).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		kaviPickList.setListContentProvider(listContentProvider);
		kaviPickList.setContentModes("normal", "history");
		kaviPickList.setContentMode(historyStore.getContentMode());
		kaviPickList.setResolvedAction(resolvedAction);
		kaviPickList.open();	
	}

	private CommandDialogPersistedSettings<QuickAccessElement> createSettingsStore(Display display, EclipseCommandProvider eclipseCommandProvider) {
		Function<HistoryKey, QuickAccessElement> historyItemResolver = historyKey -> eclipseCommandProvider.getCommand(historyKey.keys[0], historyKey.keys[1]);
		CommandDialogPersistedSettings<QuickAccessElement> historyStore = new CommandDialogPersistedSettings<>(100, item -> new HistoryKey(item.getProvider().getId(), item.getId()), historyItemResolver);
		historyStore.loadSettings();
		
		return historyStore;
	}

	private Consumer<QuickAccessElement> resolvedAction(Display display, CommandDialogPersistedSettings<QuickAccessElement> historyStore) {
		return (item) -> {
			display.asyncExec(item::execute);
			historyStore.addToHistory(item);
			historyStore.saveSettings();
		};
	}

	private ListRankAndFilter<QuickAccessElement> listRankAndFilter(FieldResolver<QuickAccessElement> labelField, FieldResolver<QuickAccessElement> providerField) {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		ListRankAndFilter<QuickAccessElement> listRankAndFilter = new ListRankAndFilter<>(
																	(filter, columnText) -> stringScore.scoreCombination(filter, columnText),
																	item -> item.getLabel());
		
		listRankAndFilter.addField(labelField.fieldId, labelField.fieldResolver);
		listRankAndFilter.addField(providerField.fieldId, providerField.fieldResolver);
		return listRankAndFilter;
	}

	private Function<InputCommand, List<RankedItem<QuickAccessElement>>> listContentProvider(ListRankAndFilter<QuickAccessElement> listRankAndFilter, CommandDialogPersistedSettings<QuickAccessElement> historyStore, EclipseCommandProvider eclipseCommandProvider) {
		
		return (inputCommand) -> {
			historyStore.setContentMode(inputCommand.contentMode);
			
			List<QuickAccessElement> historyItems = new ArrayList<>();
			for (HistoryEntry entry : historyStore.getHistory()) {
				historyItems.add((QuickAccessElement) entry.getHistoryItem());
			}
			
			if (inputCommand.contentMode.equals("history")) {

				List<QuickAccessElement> uniqueHistoryItems = historyItems.stream().distinct().collect(Collectors.toList());
				List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, uniqueHistoryItems);
				if (inputCommand.getColumnFilter(0).length() == 0)
					return listRankAndFilter.moveItem(filteredList, historyItems.get(0), 0);
				else return filteredList;
			}
			
			List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, eclipseCommandProvider.getAllCommands());
			if ((inputCommand.getColumnFilter(0).length() == 0) && (historyItems.size() > 0))
				return listRankAndFilter.moveItem(filteredList, historyItems.get(0), 0);
			else return filteredList;
		};
	}

	private FieldResolver<QuickAccessElement> providerFieldResolver() {
		FieldResolver<QuickAccessElement> providerField = new FieldResolver<>("provider",  item -> item.getProvider().getName());
		return providerField;
	}

	private FieldResolver<QuickAccessElement> labelFieldResolver() {
		FieldResolver<QuickAccessElement> labelField = new FieldResolver<>("label",  item -> item.getLabel());
		return labelField;
	}
	
	
}
