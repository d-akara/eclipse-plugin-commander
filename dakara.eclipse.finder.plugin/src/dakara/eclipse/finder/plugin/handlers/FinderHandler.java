package dakara.eclipse.finder.plugin.handlers;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import dakara.eclipse.plugin.command.settings.PersistedWorkingSet;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryKey;
import dakara.eclipse.plugin.kavi.picklist.InputState;
import dakara.eclipse.plugin.kavi.picklist.InternalCommandContextProvider;
import dakara.eclipse.plugin.kavi.picklist.InternalCommandContextProviderFactory;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.platform.EclipseWorkbench;
import dakara.eclipse.plugin.platform.ResourceItem;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

/**
 * TODO - types - org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog.TypeSearchRequestor
 * org.eclipse.jdt.core.search.TypeNameMatchRequestor
 */
public class FinderHandler extends AbstractHandler implements IStartup {
	private static PersistedWorkingSet<ResourceItem> historyStore = null;
	
	@Override
	public void earlyStartup() {
		historyStore = createSettingsStore();
		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage();
		EclipseWorkbench.createListenerForEditorFocusChanges(workbenchPage, resourceItem -> historyStore.addToHistory(resourceItem).save());
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		List<ResourceItem> files = EclipseWorkbench.collectAllWorkspaceFiles(workspace);	
		
		FieldResolver<ResourceItem> nameResolver    = new FieldResolver<>("name",    resource -> resource.name);
		FieldResolver<ResourceItem> pathResolver    = new FieldResolver<>("path",    resource -> resource.path);
		FieldResolver<ResourceItem> projectResolver = new FieldResolver<>("project", resource -> resource.project);
		
		KaviPickListDialog<ResourceItem> finder = new KaviPickListDialog<>();
		finder.setListContentProvider("discovery", listContentProvider(listRankAndFilter(nameResolver, pathResolver, projectResolver), files))
			  .setMultiResolvedAction(resourceItems -> handleSelectionAction(historyStore, workbenchPage, workspace, resourceItems))
			  .setShowAllWhenNoFilter(false)
			  .addColumn(nameResolver.fieldId, nameResolver.fieldResolver).widthPercent(30)
			  .addColumn(projectResolver.fieldId, projectResolver.fieldResolver).widthPercent(30).fontColor(155, 103, 4)
			  .addColumn(pathResolver.fieldId, pathResolver.fieldResolver).widthPercent(40).italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		
		finder.setListContentProvider("working", listContentProviderWorkingSet(listRankAndFilter(nameResolver, pathResolver, projectResolver), historyStore, files))
			  .setMultiResolvedAction(resourceItems -> handleSelectionAction(historyStore, workbenchPage, workspace, resourceItems))
			  .addColumn(nameResolver.fieldId, nameResolver.fieldResolver).widthPercent(30)
			  .addColumn(projectResolver.fieldId, projectResolver.fieldResolver).widthPercent(30).fontColor(155, 103, 4)
			  .addColumn(pathResolver.fieldId, pathResolver.fieldResolver).widthPercent(40).italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);

		
		InternalCommandContextProvider contextProvider = InternalCommandContextProviderFactory.makeProvider(finder);
		InternalCommandContextProviderFactory.addWorkingSetCommands(contextProvider, finder, historyStore);
		InternalCommandContextProviderFactory.installProvider(contextProvider, finder);
		
		finder.setCurrentProvider("working");
		finder.setBounds(800, 400);
		finder.open();	
		return null;
	}
	
	private PersistedWorkingSet<ResourceItem> createSettingsStore() {
		Function<HistoryKey, ResourceItem> historyItemResolver = historyKey -> new ResourceItem(historyKey.keys.get(0), historyKey.keys.get(2), historyKey.keys.get(1));
		PersistedWorkingSet<ResourceItem> historyStore = new PersistedWorkingSet<>(Constants.BUNDLE_ID, 100, item -> new HistoryKey(item.name, item.project, item.path), historyItemResolver);
		historyStore.load();
		
		return historyStore;
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
				IDE.openEditor(workbenchPage, workspace.getFile(Path.fromPortableString(resourceItem.project + "/" + resourceItem.path + "/" + resourceItem.name)));
			}
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Function<InputState, List<RankedItem<ResourceItem>>> listContentProvider(ListRankAndFilter<ResourceItem> listRankAndFilter, List<ResourceItem> resources) {
		
		return (inputState) -> {
			List<RankedItem<ResourceItem>> filteredList = listRankAndFilter.rankAndFilter(inputState.inputCommand, resources );
			return filteredList;
		};
	}
	
	public static Function<InputState, List<RankedItem<ResourceItem>>> listContentProviderWorkingSet(ListRankAndFilter<ResourceItem> listRankAndFilter, PersistedWorkingSet<ResourceItem> historyStore, List<ResourceItem> workspaceResources) {
		// TODO the working set recent order should be updated each time focus is changed on an open file
		return (inputState) -> {
			List<ResourceItem> workingFiles = historyStore.getHistory().stream()
														 .map(historyItem -> historyItem.getHistoryItem())
														 .filter(resourceItem -> workspaceResources.contains(resourceItem))
														 .collect(Collectors.toList());
			List<RankedItem<ResourceItem>> filteredList = listRankAndFilter.rankAndFilterOrdered(inputState.inputCommand, workingFiles);
			return filteredList;
		};
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
