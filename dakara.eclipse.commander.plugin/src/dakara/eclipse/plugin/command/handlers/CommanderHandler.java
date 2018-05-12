package dakara.eclipse.plugin.command.handlers;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.Constants;
import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryEntry;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryKey;
import dakara.eclipse.plugin.kavi.picklist.InternalCommandContextProvider;
import dakara.eclipse.plugin.kavi.picklist.InternalCommandContextProviderFactory;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;


@SuppressWarnings("restriction")
public class CommanderHandler extends AbstractHandler {
	private EclipseCommandProvider eclipseCommandProvider;
	private KaviPickListDialog<QuickAccessElement> kaviPickList;

	/* TODO's
	 * - allow other commands to reuse dialog to show other lists for faster speed
	 * - need ability to issue internal commands on lists
	 *   - for example, need command to remove an item from history
	 * 
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initialize(HandlerUtil.getActiveWorkbenchWindowChecked(event).getShell().getDisplay());
		return null;
	}
	
	public void initialize(Display display) throws ExecutionException {
		FieldResolver<QuickAccessElement> providerField = new FieldResolver<>("provider",  item -> item.getProvider().getName());
		FieldResolver<QuickAccessElement> labelField = new FieldResolver<>("label",  item -> item.getLabel());
		ListRankAndFilter<QuickAccessElement> listRankAndFilter = CommanderContentProviders.listRankAndFilter(labelField, providerField);
		
		eclipseCommandProvider = new EclipseCommandProvider();
		PersistedWorkingSet<QuickAccessElement> settingsStore = createSettingsStore(eclipseCommandProvider);
		
		kaviPickList = new KaviPickListDialog<>();
		kaviPickList.setListContentProvider("discovery", CommanderContentProviders.listContentDiscoveryProvider(listRankAndFilter, settingsStore, eclipseCommandProvider))
					.setResolvedAction(resolvedAction(display, settingsStore))
					.addColumn(labelField.fieldId, labelField.fieldResolver).widthPercent(100)
					.addColumn(providerField.fieldId, providerField.fieldResolver).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		
		kaviPickList.setListContentProvider("working",    CommanderContentProviders.listContentRecallProvider(listRankAndFilter, settingsStore, eclipseCommandProvider))
					.setResolvedAction(resolvedAction(display, settingsStore))
					.addColumn(labelField.fieldId, labelField.fieldResolver).widthPercent(100).setMarkerIndicatorProvider(item -> { 
						HistoryEntry historyEntry = settingsStore.getHistoryEntry(item);
						if (historyEntry == null) return false;
						return historyEntry.keepForever;
					})
					.addColumn(providerField.fieldId, providerField.fieldResolver).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		
		InternalCommandContextProvider contextProvider = InternalCommandContextProviderFactory.makeProvider(kaviPickList, settingsStore);
		InternalCommandContextProviderFactory.addWorkingSetCommands(contextProvider, kaviPickList, settingsStore);
		InternalCommandContextProviderFactory.addExportImportCommands(contextProvider, kaviPickList, settingsStore, "commander-settings.json");
		InternalCommandContextProviderFactory.installProvider(contextProvider, kaviPickList);
		
		kaviPickList.setBounds(600, 400);
		kaviPickList.setCurrentProvider(settingsStore.getContentMode());
		kaviPickList.setAutoCloseOnFocusLost(settingsStore.getAutoCloseFocusLost());
		kaviPickList.open();	
	}

	private PersistedWorkingSet<QuickAccessElement> createSettingsStore(EclipseCommandProvider eclipseCommandProvider) {
		Function<HistoryKey, QuickAccessElement> historyItemResolver = historyKey -> eclipseCommandProvider.getCommand(historyKey.keys.get(0), historyKey.keys.get(1));
		PersistedWorkingSet<QuickAccessElement> historyStore = new PersistedWorkingSet<>(Constants.BUNDLE_ID, false, 20, item -> new HistoryKey(item.getProvider().getId(), item.getId()), historyItemResolver);
		historyStore.load();
		
		return historyStore;
	}

	private Consumer<QuickAccessElement> resolvedAction(Display display, PersistedWorkingSet<QuickAccessElement> historyStore) {
		return (item) -> {
			display.asyncExec(item::execute);
			historyStore.addToHistory(item);
			historyStore.save();
		};
	}	
}
