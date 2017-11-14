package dakara.eclipse.plugin.kavi.picklist;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
 * - show state or status of properties in a 2nd column.
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
		
		provider.addCommand("list: selected to clipboard", (currentProvider) -> {
			
			Clipboard clipboard = new Clipboard(kaviPickList.getShell().getDisplay());
			final List<BiFunction<Object, Integer, String>> fieldResolvers = currentProvider.getKaviListColumns().getColumnOptions().stream()
					   .filter(column -> column.isSearchable())
					   .map(column -> column.getColumnContentFn())
					   .collect(Collectors.toList());
			
			FieldCollectorTransform transform = new FieldCollectorTransform(fieldResolvers, currentProvider.getSelectedEntriesImplied().stream().map(rankedItem -> rankedItem.dataItem).collect(Collectors.toList()));

			clipboard.setContents(new Object[] { transform.asAlignedColumns() },	new Transfer[] { TextTransfer.getInstance() });
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
