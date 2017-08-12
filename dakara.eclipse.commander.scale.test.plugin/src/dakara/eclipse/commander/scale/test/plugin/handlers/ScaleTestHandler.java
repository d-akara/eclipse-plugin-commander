package dakara.eclipse.commander.scale.test.plugin.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import dakara.eclipse.plugin.kavi.picklist.InputState;
import dakara.eclipse.plugin.kavi.picklist.KaviPickListDialog;
import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

public class ScaleTestHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		List<ResourceItem> files = createTestData();	
		
		FieldResolver<ResourceItem> nameResolver    = new FieldResolver<>("name",    resource -> resource.name);
		FieldResolver<ResourceItem> pathResolver    = new FieldResolver<>("path",    resource -> resource.path);
		FieldResolver<ResourceItem> projectResolver = new FieldResolver<>("project", resource -> resource.project);
		
		KaviPickListDialog<ResourceItem> finder = new KaviPickListDialog<>();
		finder.setListContentProvider("discovery", listContentProvider(listRankAndFilter(nameResolver, pathResolver, projectResolver), files))
			  .setShowAllWhenNoFilter(false)
			  .addColumn(nameResolver.fieldId, nameResolver.fieldResolver).widthPercent(30)
			  .addColumn(projectResolver.fieldId, projectResolver.fieldResolver).widthPercent(30).fontColor(155, 103, 4)
			  .addColumn(pathResolver.fieldId, pathResolver.fieldResolver).widthPercent(40).italic().fontColor(100, 100, 100).backgroundColor(250, 250, 250);

		finder.setCurrentProvider("discovery");
		
		finder.setBounds(800, 400);
		finder.open();	
		return null;
	}

	private List<ResourceItem> createTestData() {
		List<ResourceItem> files = new ArrayList<>();
		for (int index = 0; index < 1000000; index++) {
			files.add(new ResourceItem("" + index, UUID.randomUUID().toString(), "" + System.currentTimeMillis()));
		}
		return files;
	}
	
	public static Function<InputState, List<RankedItem<ResourceItem>>> listContentProvider(ListRankAndFilter<ResourceItem> listRankAndFilter, List<ResourceItem> resources) {
		
		return (inputState) -> {
			List<RankedItem<ResourceItem>> filteredList = listRankAndFilter.rankAndFilter(inputState.inputCommand, resources );
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
}
