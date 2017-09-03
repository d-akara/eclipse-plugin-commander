package dakara.eclipse.plugin.kavi.picklist;
import java.util.function.BiFunction;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import dakara.eclipse.plugin.command.settings.PersistedWorkingSet;
/*
 * TODO - copy to clipboard commands
 * - export/import history/preferences as JSON.  maybe just export/import from clipboard as first option.  Import would add to existing, not replace.
 *    - option to export hidden or non active items as well.
 * - toggle favorite
 * - show table headers, needed for resizing
 * - sort on other columns
 * - list unique by column
 *   - some way to view unique and expand items in the flat list
 * - show hidden / non active items.
 * - create alias: prepend alias name to command name or replace name entirely.
 */
public class InternalCommandContextProviderFactory {
	public static InternalCommandContextProvider makeProvider(KaviPickListDialog kaviPickList) {
		InternalCommandContextProvider provider = new InternalCommandContextProvider();
		addDefaultInternalCommands(provider, kaviPickList);
		return provider;
	}
	
	private static void addDefaultInternalCommands(InternalCommandContextProvider provider, KaviPickListDialog kaviPickList) {
		provider.addCommand("list: toggle view selected", (currentProvider) -> {
			currentProvider.toggleViewOnlySelected();
			kaviPickList.togglePreviousProvider().refreshFromContentProvider();
		});
		
		// TODO - align column output.  Include all 'searchable' columns
		provider.addCommand("list: selected to clipboard", (currentProvider) -> {
			Clipboard clipboard = new Clipboard(kaviPickList.getShell().getDisplay());
			StringBuilder builder = new StringBuilder();
			BiFunction<Object, Integer, String> columnContentFn = currentProvider.getKaviListColumns().getColumnOptions().get(1).getColumnContentFn();
			currentProvider.getSelectedEntriesImplied().stream().forEach(item -> builder.append(columnContentFn.apply(item.dataItem, 0) + "\n"));
			clipboard.setContents(new Object[] { builder.toString() },	new Transfer[] { TextTransfer.getInstance() });
			kaviPickList.togglePreviousProvider().refreshFromContentProvider();
			clipboard.dispose();
		});
		
		provider.addCommand("working", "list: toggle sort name", (currentProvider) -> {
			kaviPickList.togglePreviousProvider().sortDefault().refreshFromContentProvider();
		});		
	}
	
	public static void addWorkingSetCommands(InternalCommandContextProvider contextProvider, KaviPickListDialog kaviPickList, PersistedWorkingSet historyStore) {
		contextProvider.addCommand("working", "working: remove", (provider) -> {
			provider.getSelectedEntriesImplied().stream().map(item -> item.dataItem).forEach(item -> historyStore.removeHistory(item));
			provider.clearSelections();
			provider.clearCursor();
			kaviPickList.togglePreviousProvider().refreshFromContentProvider();
			historyStore.save();
		});
		contextProvider.addCommand("working: set favorite", (provider) -> {
			provider.getSelectedEntriesImplied().stream().map(item -> item.dataItem).forEach(item -> historyStore.setHistoryPermanent(item, true));
			provider.clearSelections();
			provider.clearCursor();
			kaviPickList.setCurrentProvider("working").refreshFromContentProvider();
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
