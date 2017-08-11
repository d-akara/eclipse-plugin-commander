package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.stringscore.RankedItem;

public class InternalContentProviderProxy<U> {
	public enum RowState {
		SELECTED(1), CURSOR(2);

		public final int value;

		RowState(int value) {
			this.value = value;
		}
	};

	private Consumer<U> resolvedActionProvider;
	private BiConsumer<U, InternalContentProviderProxy> resolvedContextActionProvider;
	private Consumer<List<U>> setMultiResolvedAction;
	private List<RankedItem<U>> tableEntries = new ArrayList<>();
	private final Set<RankedItem<U>> selectedEntries = new HashSet<>();
	private int rowCursorIndex = -1;
	private final Function<InputState, List<RankedItem<U>>> listContentProvider;
	public final String name;
	private KaviListColumns<U> kaviListColumns;
	private InputCommand previousInputCommand = null;
	private boolean restoreFilterOnChange = false;
	private boolean filterOnlySelectedEntries = false;
	private boolean showAllWhenNoFilter = true;

	public InternalContentProviderProxy(@SuppressWarnings("rawtypes") KaviList kaviList, String name,	Function<InputState, List<RankedItem<U>>> listContentProvider) {
		this.name = name;
		this.listContentProvider = listContentProvider;
	}

	public ColumnOptions<U> addColumn(String columnId, Function<U, String> columnContentFn) {
		return kaviListColumns.addColumn(columnId, (item, rowIndex) -> columnContentFn.apply(item));
	}

	public InternalContentProviderProxy<U> setResolvedAction(Consumer<U> actionResolver) {
		this.resolvedActionProvider = actionResolver;
		return this;
	}

	public InternalContentProviderProxy<U> setResolvedContextAction(BiConsumer<U, InternalContentProviderProxy> actionResolver) {
		this.resolvedContextActionProvider = actionResolver;
		return this;
	}

	public InternalContentProviderProxy<U> setMultiResolvedAction(Consumer<List<U>> setResolvedAction) {
		this.setMultiResolvedAction = setResolvedAction;
		return this;
	}

	public InternalContentProviderProxy<U> setRestoreFilterTextOnProviderChange(boolean restoreOnChange) {
		restoreFilterOnChange = restoreOnChange;
		return this;
	}

	public boolean installProvider(InternalContentProviderProxy<U> previousProvider) {
		boolean shouldRestoreFilterText = false;
		kaviListColumns.reset().installColumnsIntoTable();
		
		if (!restoreFilterOnChange && (previousProvider == null || !previousProvider.restoreFilterOnChange)) return shouldRestoreFilterText;

		shouldRestoreFilterText = true;
		return shouldRestoreFilterText;
	}

	public InputCommand previousInputCommand() {
		return previousInputCommand;
	}
	
	public InternalContentProviderProxy<U> updateTableEntries(InputState inputState) {
		if (!showAllWhenNoFilter && inputState.inputCommand.filterText.length() == 0) setTableEntries(new ArrayList<>());
		else setTableEntries(listContentProvider.apply(inputState));
		return this;
	}
	
	public InternalContentProviderProxy<U> setTableEntries(List<RankedItem<U>> tableEntries) {
		if (filterOnlySelectedEntries) {
			this.tableEntries.clear();
			for (RankedItem<U> rankedItem : tableEntries) {
				if (selectedEntries.contains(rankedItem)) this.tableEntries.add(rankedItem);
			}
		} else {
			this.tableEntries = tableEntries;
		}
		return this;
	}

	public List<RankedItem<U>> getTableEntries() {
		return this.tableEntries;
	}

	public InternalContentProviderProxy<U> moveCursorDown() {
		if (rowCursorIndex == tableEntries.size() - 1) {
			rowCursorIndex = -1;
		} else if (tableEntries.size() > rowCursorIndex + 1) {
			rowCursorIndex++;
		}

		return this;
	}

	public InternalContentProviderProxy<U> moveCursorUp() {
		if (rowCursorIndex >= 0) {
			rowCursorIndex--;
		} else {
			rowCursorIndex = tableEntries.size() - 1;
		}

		return this;
	}

	public RankedItem<U> getCursorItem() {
		if (rowCursorIndex < 0)
			return null;
		return tableEntries.get(rowCursorIndex);
	}

	public int getCursorIndex() {
		return rowCursorIndex;
	}

	public Set<RankedItem<U>> getSelectedEntries() {
		return selectedEntries;
	}
	
	public Set<RankedItem<U>> getSelectedEntriesImplied() {
		if (selectedEntries.size() > 0) return selectedEntries;
		final Set<RankedItem<U>> implied = new HashSet<>();
		if (rowCursorIndex != -1) {
			implied.add(getCursorItem());
			return implied;
		} else {
			implied.addAll(tableEntries);
		}
		return implied;
	}
	
	public int getRowIndex(RankedItem<U> rankedItem) {
		return tableEntries.indexOf(rankedItem);
	}

	public InternalContentProviderProxy<U> toggleSelectedState(RankedItem<U> item) {
		if (selectedEntries.contains(item))
			selectedEntries.remove(item);
		else
			selectedEntries.add(item);
		rowCursorIndex = tableEntries.indexOf(item);
		return this;
	}

	public InternalContentProviderProxy<U> setSelectedState(List<RankedItem<U>> items, boolean selected) {
		for (RankedItem<U> item : tableEntries) {
			setSelectedState(item, selected);
		}
		return this;
	}
	
	public InternalContentProviderProxy<U> setSelectedState(RankedItem<U> item, boolean selected) {
		if (selected)
			selectedEntries.add(item);
		else
			selectedEntries.remove(item);
		return this;
	}

	public InternalContentProviderProxy<U> toggleSelectedStateOfVisible() {

		if (isAnyVisibleItemSelected()) {
			setSelectedState(tableEntries, false);
		} else {
			selectedEntries.addAll(tableEntries);
		}
		rowCursorIndex = -1;

		return this;
	}
	
	private boolean isAnyVisibleItemSelected() {
		for (RankedItem<U> rankedItem : tableEntries) {
			if (selectedEntries.contains(rankedItem)) {
				return true;
			}
		}
		return false;
	}

	public InternalContentProviderProxy<U> selectRange(RankedItem<U> item) {
		if (rowCursorIndex < 0)
			return this;

		final int rowAnchor = rowCursorIndex;
		final boolean anchorSelected = selectedEntries.contains(tableEntries.get(rowAnchor));
		final int currentItemIndex = tableEntries.indexOf(item);
		final int rangeCount = Math.abs(rowAnchor - currentItemIndex) + 1;
		final int rangeStart = Math.min(rowCursorIndex, currentItemIndex);
		for (int rowIndex = rangeStart; rowIndex < rangeCount + rangeStart; rowIndex++) {
			setSelectedState(tableEntries.get(rowIndex), anchorSelected);
		}

		rowCursorIndex = currentItemIndex;
		return this;
	}

	public InternalContentProviderProxy<U> inverseSelectedState() {
		for (RankedItem<U> rankedItem : tableEntries) {
			if (selectedEntries.contains(rankedItem)) {
				selectedEntries.remove(rankedItem);
			} else {
				selectedEntries.add(rankedItem);
			}
		}
		return this;
	}

	public int isSelected(RankedItem<U> item) {
		int state = 0;
		if (selectedEntries.contains(item))
			state |= RowState.SELECTED.value;
		if (rowCursorIndex > -1 && tableEntries.indexOf(item) == rowCursorIndex)
			state |= RowState.CURSOR.value;

		return state;
	}

	public InternalContentProviderProxy<U> toggleViewOnlySelected() {
		filterOnlySelectedEntries = !filterOnlySelectedEntries;
		return this;
	}

	public InternalContentProviderProxy<U> clearSelections() {
		selectedEntries.clear();
		return this;
	}
	
	public InternalContentProviderProxy<U> clearPreviousInputCommand() {
		previousInputCommand = null;
		return this;
	}
	
	public boolean handleSelectionAction() {
		boolean isResolved = false;
		final RankedItem<U> selectedElement = getCursoredOrDefaultElement();
		
		List<RankedItem<U>> tableEntries = getTableEntries();
		if (tableEntries.isEmpty()) return isResolved;
		
		if (setMultiResolvedAction != null)  {
			if (selectedEntries.size() == 0) toggleSelectedState(tableEntries.get(0));
			isResolved = true;
			setMultiResolvedAction.accept(selectedEntries.stream().map(rankedItem -> rankedItem.dataItem).collect(Collectors.toList()));
		}
		// TODO temp work around until we decide how to auto select
		// get first item in the list
		if (selectedElement != null && resolvedActionProvider != null) {
			isResolved = true;
			resolvedActionProvider.accept(selectedElement.dataItem);
		}
		
		return isResolved;
	}
	
	private RankedItem<U> getCursoredOrDefaultElement() {
		RankedItem<U> selectedElement = null;
		if (getSelectedEntries().size() <= 1) {
			selectedElement = (RankedItem<U>) getCursorItem();
		}
		if ((selectedElement == null) && (tableEntries.size() > 0)) selectedElement = tableEntries.get(0);
		return selectedElement;
	}

	public boolean handleContextSelectionAction(InternalContentProviderProxy<U> previousProvider) {
		boolean isResolved = false;
		final RankedItem<U> selectedElement = getCursoredOrDefaultElement();
		if (resolvedContextActionProvider != null) {
			resolvedContextActionProvider.accept(selectedElement.dataItem, previousProvider);
			previousInputCommand = null; // clear out when we execute command
			isResolved = true;
		}
		return isResolved;
	}	
	
	public boolean filterChanged(InputCommand inputCommand)	{
		if (previousInputCommand == null) {
			previousInputCommand = inputCommand;
			return true;
		}
		boolean filterChanged = !inputCommand.isFilterEqual(previousInputCommand);
		previousInputCommand = inputCommand;
		return filterChanged;
	}
	
	public KaviListColumns<U> getKaviListColumns() {
		return kaviListColumns;
	}
	
	public InternalContentProviderProxy<U> setKaviListColumns(KaviListColumns<U> kaviListColumns) {
		this.kaviListColumns = kaviListColumns;
		return this;
	}

	public InternalContentProviderProxy<U> clearCursor() {
		rowCursorIndex = -1;
		return this;
	}
	
	public InternalContentProviderProxy<U> setShowAllWhenNoFilter(boolean showAllWhenNoFilter) {
		this.showAllWhenNoFilter = showAllWhenNoFilter;
		return this;
	}
}
