package dakara.eclipse.plugin.command.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.eclipse.internal.QuickAccessElementWithProvider;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryEntry;
import dakara.eclipse.plugin.kavi.picklist.InputState;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

public class CommanderContentProviders {
	
	public static Function<InputState, List<RankedItem<QuickAccessElementWithProvider>>> listContentDiscoveryProvider(ListRankAndFilter<QuickAccessElementWithProvider> listRankAndFilter, PersistedWorkingSet<QuickAccessElementWithProvider> historyStore, EclipseCommandProvider eclipseCommandProvider) {
		
		return (inputState) -> {
			List<RankedItem<QuickAccessElementWithProvider>> filteredList = listRankAndFilter.rankAndFilter(inputState.inputCommand, eclipseCommandProvider.getAllCommands());
			return filteredList;
		};
	}
	
	public static Function<InputState, List<RankedItem<QuickAccessElementWithProvider>>> listContentRecallProvider(ListRankAndFilter<QuickAccessElementWithProvider> listRankAndFilter, PersistedWorkingSet<QuickAccessElementWithProvider> historyStore, EclipseCommandProvider eclipseCommandProvider) {

		return (inputState) -> {
			List<QuickAccessElementWithProvider> historyItems = new ArrayList<>();
			for (HistoryEntry entry : historyStore.getHistory()) {
				historyItems.add((QuickAccessElementWithProvider) entry.getHistoryItem());
			}
			
			List<QuickAccessElementWithProvider> uniqueHistoryItems = historyItems.stream().distinct().collect(Collectors.toList());
			List<RankedItem<QuickAccessElementWithProvider>> filteredList = listRankAndFilter.rankAndFilterOrdered(inputState.inputCommand, uniqueHistoryItems);
			return filteredList;
		};
	}
	
	public static ListRankAndFilter<QuickAccessElementWithProvider> listRankAndFilter(FieldResolver<QuickAccessElementWithProvider> labelField, FieldResolver<QuickAccessElementWithProvider> providerField) {
		ListRankAndFilter<QuickAccessElementWithProvider> listRankAndFilter = ListRankAndFilter.make(labelField.fieldResolver);
		listRankAndFilter.addField(labelField.fieldId, labelField.fieldResolver);
		listRankAndFilter.addField(providerField.fieldId, providerField.fieldResolver);
		return listRankAndFilter;
	}
}
