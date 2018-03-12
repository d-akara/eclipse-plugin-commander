package dakara.eclipse.plugin.kavi.picklist;


import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.stringscore.FieldResolver;
import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;

public class InternalCommandContextProvider {
	private final List<ContextCommand> commands = new ArrayList<>();
	
	public Function<InputState, List<RankedItem<ContextCommand>>> makeProviderFunction(FieldResolver fieldResolver) {
		ListRankAndFilter<ContextCommand> listRankAndFilter = listRankAndFilter(fieldResolver);
		return (inputState) -> {
			List<RankedItem<ContextCommand>> filteredList = listRankAndFilter.rankAndFilter(inputState.inputCommand, commands );
			return filteredList.stream().filter(command -> includeCommand(command, inputState)).collect(Collectors.toList());
		};
	}
	
	private boolean includeCommand(RankedItem<ContextCommand> rankedItem, InputState inputState) {
		ContextCommand command = rankedItem.dataItem;
		if (command.mode == null) return true;
		return command.mode.equals(inputState.previousProvider.name);
	}
	
	private ListRankAndFilter<ContextCommand> listRankAndFilter(FieldResolver<ContextCommand> nameField) {
		ListRankAndFilter<ContextCommand> listRankAndFilter = ListRankAndFilter.make(nameField.fieldResolver);
		listRankAndFilter.addField(nameField.fieldId, nameField.fieldResolver);
		return listRankAndFilter;
	}
	
	public <T> InternalCommandContextProvider addCommand(String mode, String name, BiConsumer<InternalContentProviderProxy<T>, ContextCommand> handleSelections) {
		commands.add(new ContextCommand(name, mode, handleSelections));
		return this;
	}
	
	public <T> InternalCommandContextProvider addCommand(String name, BiConsumer<InternalContentProviderProxy<T>, ContextCommand> handleSelections) {
		commands.add(new ContextCommand(name, null, handleSelections));
		return this;
	}
	
	public <T> InternalCommandContextProvider addCommand(Function<ContextCommand, String> nameResolver, BiConsumer<InternalContentProviderProxy<T>, ContextCommand> handleSelections) {
		commands.add(new ContextCommand(nameResolver, null, handleSelections));
		return this;
	}	
	
	public <T> InternalCommandContextProvider addChoice(String parentName, String name, Consumer<List<T>> handleSelections) {
		// TODO find parent, add choice as child
		//commands.add(new ContextCommand<>(name, mode, handleSelections));
		return this;
	}
	
	public static class ContextCommand {
		public final String mode;
		public Function<ContextCommand, String> nameResolver;
		public final BiConsumer commandAction;
		public ContextCommand(String name, String mode, BiConsumer commandAction) {
			this.nameResolver = (command) -> name;
			this.mode = mode;
			this.commandAction = commandAction;
		}
		public ContextCommand(Function<ContextCommand, String> nameResolver, String mode, BiConsumer commandAction) {
			this.nameResolver = nameResolver;
			this.mode = mode;
			this.commandAction = commandAction;
		}
	}
	
	
	
	// add commands to provider context or global or dependent on item context
//	kaviPickList.addCommand("recall", "history: remove", (selectedItems) -> historyStore.remove(selectedItems));
//	kaviPickList.addChoice("commander initial mode:")
//				.addCommand("set history", (selectedItems) -> historyStore.remove(selectedItems))
//	            .addCommand("set normal", (selectedItems) -> historyStore.remove(selectedItems));
}
