package dakara.eclipse.plugin.command.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings.HistoryEntry;
import dakara.eclipse.plugin.kavi.picklist.InputCommand;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

@SuppressWarnings("restriction")
public class CommanderContentProvider {
	
	public static Function<InputCommand, List<RankedItem<QuickAccessElement>>> listContentDiscoveryProvider(ListRankAndFilter<QuickAccessElement> listRankAndFilter, CommandDialogPersistedSettings<QuickAccessElement> historyStore, EclipseCommandProvider eclipseCommandProvider) {
		
		return (inputCommand) -> {
			List<QuickAccessElement> historyItems = new ArrayList<>();
			for (HistoryEntry entry : historyStore.getHistory()) {
				historyItems.add((QuickAccessElement) entry.getHistoryItem());
			}
			
			List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, eclipseCommandProvider.getAllCommands());
			if ((inputCommand.getColumnFilter(0).length() == 0) && (historyItems.size() > 0))
				return listRankAndFilter.moveItem(filteredList, historyItems.get(0), 0);
			else return filteredList;
		};
	}
	
	public static Function<InputCommand, List<RankedItem<QuickAccessElement>>> listContentRecallProvider(ListRankAndFilter<QuickAccessElement> listRankAndFilter, CommandDialogPersistedSettings<QuickAccessElement> historyStore, EclipseCommandProvider eclipseCommandProvider) {

		return (inputCommand) -> {
			historyStore.setContentMode(inputCommand.contentMode);
			
			List<QuickAccessElement> historyItems = new ArrayList<>();
			for (HistoryEntry entry : historyStore.getHistory()) {
				historyItems.add((QuickAccessElement) entry.getHistoryItem());
			}
			
			List<QuickAccessElement> uniqueHistoryItems = historyItems.stream().distinct().collect(Collectors.toList());
			List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, uniqueHistoryItems);
			if (inputCommand.getColumnFilter(0).length() == 0)
				return listRankAndFilter.moveItem(filteredList, historyItems.get(0), 0);
			else return filteredList;
		};
	}
	
	public static ListRankAndFilter<QuickAccessElement> listRankAndFilter(FieldResolver<QuickAccessElement> labelField, FieldResolver<QuickAccessElement> providerField) {
		ListRankAndFilter<QuickAccessElement> listRankAndFilter = ListRankAndFilter.make(labelField.fieldResolver);
		listRankAndFilter.addField(labelField.fieldId, labelField.fieldResolver);
		listRankAndFilter.addField(providerField.fieldId, providerField.fieldResolver);
		return listRankAndFilter;
	}
}
