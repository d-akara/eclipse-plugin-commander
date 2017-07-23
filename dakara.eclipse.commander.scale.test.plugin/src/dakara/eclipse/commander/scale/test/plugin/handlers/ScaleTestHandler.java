package dakara.eclipse.commander.scale.test.plugin.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

import dakara.eclipse.plugin.kavi.picklist.InputCommand;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

/**
 * TODO - types - org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog.TypeSearchRequestor
 * org.eclipse.jdt.core.search.TypeNameMatchRequestor
 */
public class ScaleTestHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		List<ResourceItem> files = collectAllWorkspaceFiles(workspace);	
		
		FieldResolver<ResourceItem> nameResolver    = new FieldResolver<>("name",    resource -> resource.name);
		FieldResolver<ResourceItem> pathResolver    = new FieldResolver<>("path",    resource -> resource.path);
		FieldResolver<ResourceItem> projectResolver = new FieldResolver<>("project", resource -> resource.project);
		
		KaviPickListDialog<ResourceItem> finder = new KaviPickListDialog<>();
		finder.setListContentProvider("discovery", listContentProvider(listRankAndFilter(nameResolver, pathResolver, projectResolver), files))
			  .addColumn(nameResolver.fieldId, nameResolver.fieldResolver).widthPercent(30)
			  .addColumn(projectResolver.fieldId, projectResolver.fieldResolver).widthPercent(30).fontColor(155, 103, 4)
			  .addColumn(pathResolver.fieldId, pathResolver.fieldResolver).widthPercent(40).italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);

		finder.setCurrentProvider("discovery");
		
		finder.setResolvedAction(resourceItem -> openFile(workbenchPage, workspace, resourceItem));
		finder.setShowAllWhenNoFilter(false);
		finder.setBounds(800, 400);
		finder.open();	
		return null;
	}

	private List<ResourceItem> collectAllWorkspaceFiles(IWorkspaceRoot workspace) {
		List<ResourceItem> files = new ArrayList<>();
		for (int index = 0; index < 1000000; index++) {
			files.add(new ResourceItem("" + index, UUID.randomUUID().toString(), "" + System.currentTimeMillis()));
		}
		return files;
	}
	
	public static void openFile(IWorkbenchPage workbenchPage, IWorkspaceRoot workspace, ResourceItem resourceItem) {
		try {
			IDE.openEditor(workbenchPage, workspace.getFile(Path.fromPortableString(resourceItem.project + "/" + resourceItem.path + "/" + resourceItem.name)));
		} catch (PartInitException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Function<InputCommand, List<RankedItem<ResourceItem>>> listContentProvider(ListRankAndFilter<ResourceItem> listRankAndFilter, List<ResourceItem> resources) {
		
		return (inputCommand) -> {
			List<RankedItem<ResourceItem>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, resources );
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
	
	public static String makePathOnly(IPath path) {
		return path.removeLastSegments(1).toString();
	}
	
	public static class ResourceItem {
		public final String name;
		public final String path;
		public final String project;
		public ResourceItem(String name, String path, String project) {
			this.name = name;
			this.path = path;
			this.project = project;
		}
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
