package dakara.eclipse.plugin.kavi.picklist;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.FileDialog;

import dakara.eclipse.plugin.command.Constants;
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
import dakara.eclipse.plugin.log.EclipsePluginLogger;

public class InternalCommandContextProviderFactory {
	private static EclipsePluginLogger logger = new EclipsePluginLogger(Constants.BUNDLE_ID);	
	
	public static InternalCommandContextProvider makeProvider(KaviPickListDialog kaviPickList) {
		InternalCommandContextProvider provider = new InternalCommandContextProvider();
		addDefaultInternalCommands(provider, kaviPickList);
		return provider;
	}
	
	private static void placeOnClipboard(KaviPickListDialog kaviPickList, String contents) {
		Clipboard clipboard = new Clipboard(kaviPickList.getShell().getDisplay());
		clipboard.setContents(new Object[] { contents },	new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
	}
	
	private static String receiveFromClipboard(KaviPickListDialog kaviPickList) {
		Clipboard clipboard = new Clipboard(kaviPickList.getShell().getDisplay());
		String contents = (String) clipboard.getContents(TextTransfer.getInstance());
		clipboard.dispose();
		return contents;
	}	
	
	private static void addDefaultInternalCommands(InternalCommandContextProvider provider, KaviPickListDialog kaviPickList) {
		provider.addCommand("list: toggle view selected", (currentProvider) -> {
			currentProvider.toggleViewOnlySelected();
			kaviPickList.togglePreviousProvider().refreshFromContentProvider();
		});
		
		provider.addCommand("list: selected to clipboard", (currentProvider) -> {
			
			final List<BiFunction<Object, Integer, String>> fieldResolvers = currentProvider.getKaviListColumns().getColumnOptions().stream()
					   .filter(column -> column.isSearchable())
					   .map(column -> column.getColumnContentFn())
					   .collect(Collectors.toList());
			
			FieldCollectorTransform transform = new FieldCollectorTransform(fieldResolvers, currentProvider.getSelectedEntriesImplied().stream().map(rankedItem -> rankedItem.dataItem).collect(Collectors.toList()));

			placeOnClipboard(kaviPickList, transform.asAlignedColumns());
			kaviPickList.togglePreviousProvider().refreshFromContentProvider();
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
	
	public static void addExportImportCommands(InternalCommandContextProvider contextProvider, KaviPickListDialog kaviPickList, PersistedWorkingSet historyStore, String settingsFilename) {
		contextProvider.addCommand("working", "export: settings as JSON to file", (currentProvider) -> {
		    FileDialog dialog = new FileDialog(kaviPickList.getShell(), SWT.SAVE);
		    dialog.setFilterExtensions(new String [] {"*.json"});
		    dialog.setFileName(settingsFilename);
		    String filename = dialog.open();
		    if (filename == null) return;
		    
		    try {
				Files.write(Paths.get(filename), historyStore.settingsAsJson().getBytes(StandardCharsets.UTF_8));
				kaviPickList.togglePreviousProvider().refreshFromContentProvider();
				MessageDialog.openInformation(kaviPickList.getShell(), "Export Settings", "Saved settings as " + dialog.getFileName());
			} catch (IOException e) {
				logger.error("unable to save JSON", e);
			}
		});	
		
		// TODO - add a version of import which will merge settings
		// This may remove the need to keep files across workspaces since a user can easily export and merge.
		// That would solve our startup performance problem on ultra large workspaces as well.
		// This means that the history items will need to be per workspace, but only for Finder.
		// I think across workspace for Commands is still appropriate
		// Also, we may need to then disable the builtin eclipse preferences export and import.
		// Otherwise we will overwrite our settings on import, defeating our merge capability.  Need to consider carefully
		contextProvider.addCommand("working", "import: settings as JSON from file", (currentProvider) -> {
		    FileDialog dialog = new FileDialog(kaviPickList.getShell(), SWT.OPEN);
		    dialog.setFilterExtensions(new String [] {"*.json"});
		    dialog.setFileName(settingsFilename);
		    String filename = dialog.open();
		    try {
				String json = new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
				historyStore.setSettingsFromJson(json);
				historyStore.save();
				currentProvider.clearSelections();
				currentProvider.clearCursor();
				kaviPickList.togglePreviousProvider().refreshFromContentProvider();
			} catch (Throwable e) {
				logger.error("unable to import JSON", e);
				MessageDialog.openError(kaviPickList.getShell(), "Import Failed", e.getMessage());
			}
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
