package dakara.eclipse.plugin.kavi.picklist;

import java.util.Arrays;
import java.util.List;

public class InputCommand {
	public final String filterText;
	private final List<String> columnFilters;
	public final String fastSelectIndex;
	public final boolean fastSelect;
	public final boolean multiSelect;
	public final boolean selectRange;
	public final boolean isColumnFiltering;
	public final boolean inverseSelection;
	public final boolean selectAll;
	public final ListType listType;
	public enum ListType {
		CONTENT,
		INTERNAL_COMMAND
	};
	public InputCommand(String filterText, List<String> columnFilters, String fastSelectIndex, boolean fastSelect, boolean multiSelect, boolean selectRange, boolean inverseSelection, boolean selectAll, boolean isColumnFiltering, ListType listType) {
		this.filterText = filterText;
		this.columnFilters = columnFilters;
		this.fastSelectIndex = fastSelectIndex;
		this.fastSelect = fastSelect;
		this.multiSelect = multiSelect;
		this.selectRange = selectRange;
		this.inverseSelection = inverseSelection;
		this.selectAll = selectAll;
		this.isColumnFiltering = isColumnFiltering;
		this.listType = listType;
	}
	
	public String getColumnFilter(final int column) {
		// When we only have 1 filter it should be applied to all columns
		if (!isColumnFiltering) return columnFilters.get(0);
		
		if (column >= columnFilters.size()) return "";
		
		return columnFilters.get(column);
	}
	
	public static InputCommand parse(String inputText) {
		return InputCommand.makeInputCommand(inputText);
	}
	
	public boolean isFilterEqual(InputCommand otherInput) {
		return columnFilters.equals(otherInput.columnFilters) && isColumnFiltering == otherInput.isColumnFiltering;
	}

	private static InputCommand makeInputCommand(String commandPart) {
		boolean fastSelectActive = commandPart.contains("/");
		boolean multiSelectActive = commandPart.contains("//");
		boolean isColumnFiltering = commandPart.contains(",");
		
		String[] splitPart = commandPart.split("/");
		String[] columnFilters;
		if (splitPart.length == 0) columnFilters = new String[]{""};
		else columnFilters = splitPart[0].split(",");
				
		String fastSelect = null;
		if (splitPart.length == 2) fastSelect = splitPart[1];
		if (splitPart.length == 3) fastSelect = splitPart[2];  // multi select
		
		boolean selectRange = false;
		boolean inverseSelection = false;
		boolean selectAll = false;
		if (fastSelect != null) {
			if (fastSelect.startsWith("-")) {
				selectRange = true;
				fastSelect = fastSelect.substring(1);
			} else if (fastSelect.startsWith("!")) {
				fastSelect = fastSelect.substring(1);
				inverseSelection = true;
			} else if (fastSelect.startsWith(" ")) {
				fastSelect = fastSelect.substring(1);
				selectAll = true;
			} 
		}
		return new InputCommand(commandPart, Arrays.asList(columnFilters), fastSelect, fastSelectActive, multiSelectActive, selectRange, inverseSelection, selectAll, isColumnFiltering, ListType.CONTENT);
	}
}
