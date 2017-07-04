package dakara.eclipse.finder.plugin.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
import dakara.eclipse.plugin.stringscore.StringScore;
import dakara.eclipse.plugin.stringscore.StringScoreRanking;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class FinderHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPage workbenchPage = HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage();
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		List<ResourceItem> files = new ArrayList<>();
		
		IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
			public boolean visit(IResourceProxy proxy) throws CoreException {
				if (proxy.getType() != IResource.FILE) return true;
				if (proxy.isDerived()) return false;
				if (proxy.isPhantom()) return false;
				if (proxy.isHidden()) return false;
				IFile file = (IFile) proxy.requestResource();
				if (file.getProjectRelativePath().segment(0).equals("indices")) return false;
				files.add(new ResourceItem(file.getName(), makePathOnly(file.getProjectRelativePath()), file.getProject().getName()));
				return false;
			}
		};
		
		try {
			IResource[] resources = workspace.members();
			for(IResource resource : resources) {
				if (!resource.getProject().isOpen()) continue;
				resource.accept(visitor, 0);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}	
		
		FieldResolver<ResourceItem> nameResolver = new FieldResolver<>("name", resource -> resource.name);
		FieldResolver<ResourceItem> pathResolver = new FieldResolver<>("path", resource -> resource.path);
		FieldResolver<ResourceItem> projectResolver = new FieldResolver<>("project", resource -> resource.project);
		
		KaviPickListDialog<ResourceItem> finder = new KaviPickListDialog<>();
		finder.addColumn(nameResolver.fieldId, nameResolver.fieldResolver).width(200);
		finder.addColumn(projectResolver.fieldId, projectResolver.fieldResolver).width(200);
		finder.addColumn(pathResolver.fieldId, pathResolver.fieldResolver).width(300).italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);
		
		finder.setListContentProvider(listContentProvider(listRankAndFilter(nameResolver, pathResolver, projectResolver), files));
		finder.setResolvedAction(resourceItem -> openFile(workbenchPage, workspace, resourceItem));
		finder.setShowAllWhenNoFilter(false);
		finder.open();	
		return null;
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
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		ListRankAndFilter<ResourceItem> listRankAndFilter = new ListRankAndFilter<>(
																	(filter, columnText) -> stringScore.scoreCombination(filter, columnText),
																	nameField.fieldResolver);
		
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
