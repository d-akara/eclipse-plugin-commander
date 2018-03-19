package dakara.eclipse.finder.plugin.handlers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.Openable;
import org.eclipse.jdt.internal.core.util.HandleFactory;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import dakara.eclipse.plugin.command.settings.PersistedWorkingSet;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryEntry;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryKey;
import dakara.eclipse.plugin.kavi.picklist.InputState;
import dakara.eclipse.plugin.kavi.picklist.InternalCommandContextProvider;
import dakara.eclipse.plugin.kavi.picklist.InternalCommandContextProviderFactory;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.log.EclipsePluginLogger;
import dakara.eclipse.plugin.platform.EclipseWorkbench;
import dakara.eclipse.plugin.platform.ResourceItem;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

/**
 * TODO -  add types, either here or as another dialog - org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog.TypeSearchRequestor
 * - add eclipse context commands for files.  Same options as viewing from package explorer
 * - track edited files as another attribute in history.  So we can list all files we have edited.
 * - add markers for files edited, files currently open
 * - filter by files edited
 * 
 * org.eclipse.jdt.core.search.TypeNameMatchRequestor
 */
public class FinderHandler extends AbstractHandler implements IStartup {
	private static EclipsePluginLogger logger = new EclipsePluginLogger(Constants.BUNDLE_ID);
	private boolean initialized = false;
	private PersistedWorkingSet<ResourceItem> historyStore = null;
	private List<ResourceItem> files = null;
	private long lastResourceRefresh = 0l;
	
	// NOTE: early startup creates another instance of this class separate from the instance used for execute
	// we should do this differently
	@Override
	public void earlyStartup() {
	}
	
	private void initialize() {
		if (!initialized) {
			initialized = true;
			historyStore = createSettingsStore();
			IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage();
			EclipseWorkbench.createListenerForEditorFocusChanges(workbenchPage, resourceItem -> historyStore.addToHistory(resourceItem).save());
			
			EclipseWorkbench.notifyResourceAddedOrRemoved(() -> {
				files = null;
				logger.info("resource changed, clearing cache");
			});		
		}
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initialize();
		invalidateTypesIfNeeded();
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		
		FieldResolver<ResourceItem> nameResolver    = new FieldResolver<>("name",    resource -> resource.name);
		FieldResolver<ResourceItem> pathResolver    = new FieldResolver<>("path",    resource -> extractPath(resource.path));
		FieldResolver<ResourceItem> projectResolver = new FieldResolver<>("project", resource -> resource.project);
		
		KaviPickListDialog<ResourceItem> finder = new KaviPickListDialog<>();
		finder.setListContentProvider("discovery", listContentProvider(listRankAndFilter(nameResolver, pathResolver, projectResolver), this::getAllFileAndTypeResources))
			  .setMultiResolvedAction(resourceItems -> handleSelectionAction(historyStore, workbenchPage, workspace, resourceItems))
			  .setShowAllWhenNoFilter(false)
			  .setDebounceTimeProvider(inputCommand -> inputCommand.countFilterableCharacters() > 2 ? 50:200)
			  .addColumn(nameResolver.fieldId, nameResolver.fieldResolver).widthPercent(30)
			  .addColumn(projectResolver.fieldId, projectResolver.fieldResolver).widthPercent(30).fontColor(155, 103, 4)
			  .addColumn(pathResolver.fieldId, pathResolver.fieldResolver).widthPercent(40).italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		
		finder.setListContentProvider("working", listContentProviderWorkingSet(listRankAndFilter(nameResolver, pathResolver, projectResolver), historyStore))
			  .setMultiResolvedAction(resourceItems -> handleSelectionAction(historyStore, workbenchPage, workspace, resourceItems))
			  .addColumn(nameResolver.fieldId, nameResolver.fieldResolver).widthPercent(30).setMarkerIndicatorProvider(item -> { 
					HistoryEntry historyEntry = historyStore.getHistoryEntry(item);
					if (historyEntry == null) return false;
					return historyEntry.keepForever;
				})
			  .addColumn(projectResolver.fieldId, projectResolver.fieldResolver).widthPercent(30).fontColor(155, 103, 4)
			  .addColumn(pathResolver.fieldId, pathResolver.fieldResolver).widthPercent(40).italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);

		
		InternalCommandContextProvider contextProvider = InternalCommandContextProviderFactory.makeProvider(finder, historyStore);
		InternalCommandContextProviderFactory.addWorkingSetCommands(contextProvider, finder, historyStore);
		InternalCommandContextProviderFactory.addExportImportCommands(contextProvider, finder, historyStore, "finder-settings_" + EclipseWorkbench.workspaceName() + ".json");
		InternalCommandContextProviderFactory.installProvider(contextProvider, finder);
		
		finder.setCurrentProvider(historyStore.getContentMode());
		finder.setBounds(800, 400);
		finder.open();	
		return null;
	}
	
	private List<ResourceItem> getAllFileAndTypeResources() {
		if (files == null) {
			lastResourceRefresh = System.currentTimeMillis();
			logger.info("loading all resources and types from workspace");
			files = EclipseWorkbench.collectAllWorkspaceFiles();	
			files.addAll(EclipseWorkbench.collectAllWorkspaceTypes());
		}
		return files;
	}
	
	private void invalidateTypesIfNeeded() {
		if (!EclipseWorkbench.hasWorkspaceTypesChanged(lastResourceRefresh)) return;
		logger.info("types have changed, invalidating cache");
		files = null;
	}
	
	private PersistedWorkingSet<ResourceItem> createSettingsStore() {
		Function<HistoryKey, ResourceItem> historyItemResolver = historyKey -> new ResourceItem(historyKey.keys.get(0), historyKey.keys.get(2), historyKey.keys.get(1));
		PersistedWorkingSet<ResourceItem> historyStore = new PersistedWorkingSet<>(Constants.BUNDLE_ID, true, 100, item -> new HistoryKey(item.name, item.project, item.path), historyItemResolver);
		historyStore.load();
		
		return historyStore;
	}
	
	private String extractPath(String jarPathAndClass) {
		int startLocation = 0;
		int endLocation = jarPathAndClass.length();
		
		final int locationOfSeparator = jarPathAndClass.indexOf("|");
		if (locationOfSeparator >= 0) startLocation = locationOfSeparator + 1;
		
		final int locationOfClass = jarPathAndClass.lastIndexOf(("/"));
		if (locationOfClass >=0 ) endLocation = locationOfClass;

		if (endLocation < startLocation) return ""; // There was no path
		
		return jarPathAndClass.substring(startLocation, endLocation);
	}
	
	public static void handleSelectionAction(PersistedWorkingSet<ResourceItem> historyStore, IWorkbenchPage workbenchPage, IWorkspaceRoot workspace, List<ResourceItem> resourceItems) {
		for(ResourceItem resourceItem : resourceItems) {
			historyStore.addToHistory(resourceItem);
		}
		historyStore.save();
		openFile(workbenchPage, workspace, resourceItems);
	}
	
	public static void openFile(IWorkbenchPage workbenchPage, IWorkspaceRoot workspace, List<ResourceItem> resourceItems) {
		try {
			for (ResourceItem resourceItem : resourceItems) {
				if (resourceItem.name.endsWith(".class"))
					try {
						HandleFactory factory = new HandleFactory();
						Openable openable = factory.createOpenable(resourceItem.path, null);
						IType classFile = ((IClassFile)openable).getType();
						JavaUI.openInEditor(classFile, true, true);
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				else
					IDE.openEditor(workbenchPage, workspace.getFile(Path.fromPortableString(resourceItem.project + "/" + resourceItem.path + "/" + resourceItem.name)));
			}
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Function<InputState, List<RankedItem<ResourceItem>>> listContentProvider(ListRankAndFilter<ResourceItem> listRankAndFilter, Supplier<List<ResourceItem>> resourceProvider) {
		
		return (inputState) -> {
			// TODO pre filter lists based on scope
			// but how do we update pre filter here when scope changes?
			List<RankedItem<ResourceItem>> filteredList = listRankAndFilter.rankAndFilter(inputState.inputCommand, resourceProvider.get() );
			return filteredList;
		};
	}
	
	public static Function<InputState, List<RankedItem<ResourceItem>>> listContentProviderWorkingSet(ListRankAndFilter<ResourceItem> listRankAndFilter, PersistedWorkingSet<ResourceItem> historyStore) {
		AtomicReference<List<PersistedWorkingSet<ResourceItem>.HistoryEntry>> historyItems = new AtomicReference(historyStore.getHistory());
		AtomicReference<List<ResourceItem>> workingFiles = new AtomicReference(getCurrentHistoryItems(historyItems.get()));
		return (inputState) -> {
			List<PersistedWorkingSet<ResourceItem>.HistoryEntry> currentHistoryItems = historyStore.getHistory();
			// Has the history changed since last time
			if (currentHistoryItems != historyItems.get()) {
				logger.info("history changed");
				historyItems.set(currentHistoryItems);
				workingFiles.set(getCurrentHistoryItems(historyItems.get()));
			}

			List<RankedItem<ResourceItem>> filteredList = listRankAndFilter.rankAndFilterOrdered(inputState.inputCommand, workingFiles.get());
			return filteredList;
		};
	}

	private static List<ResourceItem> getCurrentHistoryItems(List<PersistedWorkingSet<ResourceItem>.HistoryEntry> historyItems) {
		List<ResourceItem> workingFiles = historyItems.stream()
				.map(historyItem -> historyItem.getHistoryItem())
				.collect(Collectors.toList());
		return workingFiles;
	}
	
	public static ListRankAndFilter<ResourceItem> listRankAndFilter(FieldResolver<ResourceItem> nameField, FieldResolver<ResourceItem> pathField, FieldResolver<ResourceItem> projectField) {
		ListRankAndFilter<ResourceItem> listRankAndFilter = ListRankAndFilter.make(nameField.fieldResolver);
		listRankAndFilter.addField(nameField.fieldId, nameField.fieldResolver);
		listRankAndFilter.addField(projectField.fieldId, projectField.fieldResolver);
		listRankAndFilter.addField(pathField.fieldId, pathField.fieldResolver);
		return listRankAndFilter;
	}
	
//    IFile file = getFileResource();
//    if (file == null) {
//        return;
//    }
//    try {
//    	if (openUsingDescriptor) {
//    		((WorkbenchPage) page).openEditorFromDescriptor(new FileEditorInput(file), editorDescriptor, true, null);
//    	} else {
//            String editorId = editorDescriptor == null ? IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID
//                    : editorDescriptor.getId();
//
//            page.openEditor(new FileEditorInput(file), editorId, true, MATCH_BOTH);
//            // only remember the default editor if the open succeeds
//            IDE.setDefaultEditor(file, editorId);
//    	}	
	
}
