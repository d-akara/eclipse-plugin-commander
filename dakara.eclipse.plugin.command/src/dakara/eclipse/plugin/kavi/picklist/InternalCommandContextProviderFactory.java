package dakara.eclipse.plugin.kavi.picklist;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet;
import dakara.eclipse.plugin.kavi.picklist.InternalCommandContextProvider.ContextCommand;

public class InternalCommandContextProviderFactory {
	public static InternalCommandContextProvider makeProvider(KaviPickListDialog kaviPickList) {
		InternalCommandContextProvider provider = new InternalCommandContextProvider();
		addDefaultInternalCommands(provider, kaviPickList);
		return provider;
	}
	
	private static void addDefaultInternalCommands(InternalCommandContextProvider provider, KaviPickListDialog kaviPickList) {
		provider.addCommand("list: toggle view selected", (InternalContentProviderProxy<Object> currentProvider) -> {
			currentProvider.toggleViewOnlySelected();
			kaviPickList.togglePreviousProvider();
		});
	}
	
	public static void addWorkingSetCommands(InternalCommandContextProvider contextProvider, KaviPickListDialog kaviPickList, PersistedWorkingSet historyStore) {
		contextProvider.addCommand("working", "working: remove", (InternalContentProviderProxy<Object> provider) -> {
			provider.getSelectedEntriesImplied().stream().map(item -> item.dataItem).forEach(item -> historyStore.removeHistory(item));
			provider.clearSelections();
			provider.clearCursor();
			kaviPickList.togglePreviousProvider();
			historyStore.save();
		});
		contextProvider.addCommand("working: set favorite", (InternalContentProviderProxy<Object> provider) -> {
			provider.getSelectedEntriesImplied().stream().map(item -> item.dataItem).forEach(item -> historyStore.setHistoryPermanent(item, true));
			provider.clearSelections();
			provider.clearCursor();
			kaviPickList.setCurrentProvider("working");
			historyStore.save();
		});
	}
	
	public static void installProvider(InternalCommandContextProvider contextProvider, KaviPickListDialog<? extends Object> kaviPickList) {
		kaviPickList.setListContentProvider("context", contextProvider.makeProviderFunction()).setRestoreFilterTextOnProviderChange(true)
        .setResolvedContextAction(( command, provider) -> {
        	command.commandAction.accept(provider);
        })
        .addColumn("name", item -> item.name).widthPercent(100);
	}
}
