package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

public class InternalCommandContextProvider {
	private final List<ContextCommand> commands = new ArrayList<>();
	
	public Function<InputCommand, List<RankedItem<ContextCommand>>> makeProviderFunction() {
		ListRankAndFilter<ContextCommand> listRankAndFilter = listRankAndFilter(new FieldResolver<ContextCommand>("name", item -> item.name ));
		return (inputCommand) -> {
			List<RankedItem<ContextCommand>> filteredList = listRankAndFilter.rankAndFilter(inputCommand, commands );
			return filteredList;
		};
	}
	
	private ListRankAndFilter<ContextCommand> listRankAndFilter(FieldResolver<ContextCommand> nameField) {
		ListRankAndFilter<ContextCommand> listRankAndFilter = ListRankAndFilter.make(nameField.fieldResolver);
		listRankAndFilter.addField(nameField.fieldId, nameField.fieldResolver);
		return listRankAndFilter;
	}
	
	public InternalCommandContextProvider addCommand(String mode, String name, Consumer<List> handleSelections) {
		commands.add(new ContextCommand<>(name, mode, handleSelections));
		return this;
	}
	
	public InternalCommandContextProvider addChoice(String parentName, String name, Consumer<List> handleSelections) {
		// TODO find parent, add choice as child
		//commands.add(new ContextCommand<>(name, mode, handleSelections));
		return this;
	}
	
	public static class ContextCommand<T> {
		public final String mode;
		public final String name;
		public final Consumer<List> handleSelections;
		public ContextCommand(String name, String mode, Consumer<List> handleSelections) {
			this.name = name;
			this.mode = mode;
			this.handleSelections = handleSelections;
		}
	}
	
	
	
	// add commands to provider context or global or dependent on item context
//	kaviPickList.addCommand("recall", "history: remove", (selectedItems) -> historyStore.remove(selectedItems));
//	kaviPickList.addChoice("commander initial mode:")
//				.addCommand("set history", (selectedItems) -> historyStore.remove(selectedItems))
//	            .addCommand("set normal", (selectedItems) -> historyStore.remove(selectedItems));
}
