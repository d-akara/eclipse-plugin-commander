package dakara.eclipse.plugin.command.handlers;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

import dakara.eclipse.plugin.command.eclipse.internal.EclipseCommandProvider;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings;
import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings.HistoryKey;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;


@SuppressWarnings("restriction")
public class CommanderHandler extends AbstractHandler {
	private EclipseCommandProvider eclipseCommandProvider;
	private KaviPickListDialog<QuickAccessElement> kaviPickList;

	/* TODO's
	 * - Allow content modes to have separate columns and content provider
	 * - Add commands to a content mode.
	 * - allow other commands to reuse dialog to show other lists for faster speed
	 * - need ability to issue internal commands on lists
	 *   - for example, need command to remove an item from history
	 * - Resource version of KAVA - ResourcesPlugin.getWorkspace().getRoot()
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
		ListRankAndFilter<QuickAccessElement> listRankAndFilter = CommanderContentProvider.listRankAndFilter(labelField, providerField);
		
		eclipseCommandProvider = new EclipseCommandProvider();
		CommandDialogPersistedSettings<QuickAccessElement> historyStore = createSettingsStore(display, eclipseCommandProvider);
		
		kaviPickList = new KaviPickListDialog<>();
		kaviPickList.addColumn(labelField.fieldId, labelField.fieldResolver).width(520);
		kaviPickList.addColumn(providerField.fieldId, providerField.fieldResolver).width(85).right().italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		kaviPickList.setListContentProvider(CommanderContentProvider.listContentProvider(listRankAndFilter, historyStore, eclipseCommandProvider));
//		kaviPickList.addCommand("history: remove", (selectedItems) -> historyStore.remove(selectedItems));
//		kaviPickList.addChoice("commander initial mode:")
//					.addCommand("set history", (selectedItems) -> historyStore.remove(selectedItems))
//		            .addCommand("set normal", (selectedItems) -> historyStore.remove(selectedItems));
		kaviPickList.setContentModes("discovery", "recall");
		kaviPickList.setContentMode(historyStore.getContentMode());
		kaviPickList.setResolvedAction(resolvedAction(display, historyStore));
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

	private FieldResolver<QuickAccessElement> providerFieldResolver() {
		FieldResolver<QuickAccessElement> providerField = new FieldResolver<>("provider",  item -> item.getProvider().getName());
		return providerField;
	}

	private FieldResolver<QuickAccessElement> labelFieldResolver() {
		FieldResolver<QuickAccessElement> labelField = new FieldResolver<>("label",  item -> item.getLabel());
		return labelField;
	}
	
	
}
