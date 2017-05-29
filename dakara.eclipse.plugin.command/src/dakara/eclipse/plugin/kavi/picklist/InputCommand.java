package dakara.eclipse.plugin.kavi.picklist;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputCommand {
	private final List<String> filter;
	public final String fastSelectIndex;
	public final boolean fastSelect;
	public final boolean isColumnFiltering;
	public final ListType listType;
	public enum ListType {
		CONTENT,
		INTERNAL_COMMAND
	};
	public InputCommand(List<String> filter, String fastSelectIndex, boolean fastSelect, boolean isColumnFiltering, ListType listType) {
		this.filter = filter;
		this.fastSelectIndex = fastSelectIndex;
		this.fastSelect = fastSelect;
		this.isColumnFiltering = isColumnFiltering;
		this.listType = listType;
	}
	
	public String getColumnFilter(final int column) {
		// When we only have 1 filter it should be applied to all columns
		if (!isColumnFiltering) return filter.get(0);
		
		if (column >= filter.size()) return "";
		
		return filter.get(column);
	}
	
	public static List<InputCommand> parse(String inputText) {
		// split on commands
		return Stream.of(inputText.split(":"))
			.map(InputCommand::makeInputCommand)
			.collect(Collectors.toList());
	}
	
	public boolean isFilterEqual(InputCommand otherInput) {
		return filter.equals(otherInput.filter) && isColumnFiltering == otherInput.isColumnFiltering;
	}

	private static InputCommand makeInputCommand(String commandPart) {
		boolean fastSelectActive = commandPart.contains("/");
		boolean isColumnFiltering = commandPart.contains("|");
		
		String[] splitPart = commandPart.split("/");
		String[] filters = splitPart[0].split("\\|");
				
		String fastSelect = null;
		if (splitPart.length == 2) {
			fastSelect = splitPart[1];
		}
		
		return new InputCommand(Arrays.asList(filters), fastSelect, fastSelectActive, isColumnFiltering, ListType.CONTENT);
	}
}
