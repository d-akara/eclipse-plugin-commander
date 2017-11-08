package dakara.eclipse.plugin.command.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryEntry;
import dakara.eclipse.plugin.kavi.picklist.InputState;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

@SuppressWarnings("restriction")
public class CommanderContentProviders {
	
	public static Function<InputState, List<RankedItem<QuickAccessElement>>> listContentDiscoveryProvider(ListRankAndFilter<QuickAccessElement> listRankAndFilter, PersistedWorkingSet<QuickAccessElement> historyStore, EclipseCommandProvider eclipseCommandProvider) {
		
		return (inputState) -> {
			List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilter(inputState.inputCommand, eclipseCommandProvider.getAllCommands());
			return filteredList;
		};
	}
	
	public static Function<InputState, List<RankedItem<QuickAccessElement>>> listContentRecallProvider(ListRankAndFilter<QuickAccessElement> listRankAndFilter, PersistedWorkingSet<QuickAccessElement> historyStore, EclipseCommandProvider eclipseCommandProvider) {

		return (inputState) -> {
			List<QuickAccessElement> historyItems = new ArrayList<>();
			for (HistoryEntry entry : historyStore.getHistory()) {
				historyItems.add((QuickAccessElement) entry.getHistoryItem());
			}
			
			List<QuickAccessElement> uniqueHistoryItems = historyItems.stream().distinct().collect(Collectors.toList());
			List<RankedItem<QuickAccessElement>> filteredList = listRankAndFilter.rankAndFilterOrdered(inputState.inputCommand, uniqueHistoryItems);
			return filteredList;
		};
	}
	
	public static ListRankAndFilter<QuickAccessElement> listRankAndFilter(FieldResolver<QuickAccessElement> labelField, FieldResolver<QuickAccessElement> providerField) {
		ListRankAndFilter<QuickAccessElement> listRankAndFilter = ListRankAndFilter.make(labelField.fieldResolver);
		listRankAndFilter.addField(labelField.fieldId, labelField.fieldResolver);
		listRankAndFilter.addField(providerField.fieldId, providerField.fieldResolver);
		return listRankAndFilter;
	}
}
