package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	private int rowCursorIndex = 0;
	private final Function<InputState, List<RankedItem<U>>> listContentProvider;
	public final String name;
	private KaviListColumns<U> kaviListColumns;
	private InputState previousInputState = null;
	private boolean restoreFilterOnChange = false;
	private boolean showAllWhenNoFilter = true;
	private Function<Stream<RankedItem<U>>, Stream<RankedItem<U>>> sortResolverFn;
	private final Map<String, Function<Stream<RankedItem<U>>, Stream<RankedItem<U>>>> filterResolvers = new HashMap<>();
	private Function<InputCommand, Integer> debounceTimeProvider;

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
	
	public InternalContentProviderProxy<U> setDebounceTimeProvider(Function<InputCommand, Integer> debounceTimeProvider) {
		this.debounceTimeProvider = debounceTimeProvider;
		return this;
	}
	
	public int calculateDebounceTime(InputCommand command) {
		if (debounceTimeProvider == null) return 0;
		return debounceTimeProvider.apply(command);
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
		if (previousInputState == null) return null;
		return previousInputState.inputCommand;
	}
	
	public InternalContentProviderProxy<U> updateTableEntries(InputState inputState) {
		final boolean filterChanged = filterChanged(inputState);
		
		if (filterChanged) rowCursorIndex = 0;
		
		if (!showAllWhenNoFilter && inputState.inputCommand.filterText.length() == 0 && !inputState.inputCommand.fastSelect) setTableEntries(new ArrayList<>());
		else if (!filterChanged) return this;
		else setTableEntries(listContentProvider.apply(inputState));
		return this;
	}
	
	public InternalContentProviderProxy<U> refreshFromContentProvider() {
		setTableEntries(listContentProvider.apply(previousInputState));
		return this;
	}
	
	public InternalContentProviderProxy<U> setTableEntries(List<RankedItem<U>> tableEntries) {
		// TODO optimize and only create a stream when necessary
		Stream<RankedItem<U>> tableStream = tableEntries.parallelStream();
		if (sortResolverFn != null) {
			tableStream = sortResolverFn.apply(tableStream);
		}
		
		tableStream = applyFilters(tableStream);
		
		this.tableEntries = tableStream.collect(Collectors.toList());
		return this;
	}
	
	private Stream<RankedItem<U>> applyFilters(Stream<RankedItem<U>> stream) {
		if (filterResolvers.isEmpty()) return stream;
		for (Function<Stream<RankedItem<U>>, Stream<RankedItem<U>>> filterResolver : filterResolvers.values()) {
			stream = filterResolver.apply(stream);
		}
		
		return stream;
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
	
	public void setCursorIndex(int rowCursorIndex) {
		this.rowCursorIndex = rowCursorIndex;
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

	public int itemRowState(RankedItem<U> item) {
		int state = 0;
		if (selectedEntries.contains(item))
			state |= RowState.SELECTED.value;
		if (rowCursorIndex > -1 && tableEntries.indexOf(item) == rowCursorIndex)
			state |= RowState.CURSOR.value;

		return state;
	}

	public InternalContentProviderProxy<U> toggleViewOnlySelected() {
		if (!filterResolvers.containsKey("filterSelected"))
			filterResolvers.put("filterSelected", stream -> stream.filter(item -> selectedEntries.contains(item)));
		else filterResolvers.remove("filterSelected");
		return this;
	}

	public InternalContentProviderProxy<U> clearSelections() {
		selectedEntries.clear();
		return this;
	}
	
	public InternalContentProviderProxy<U> clearPreviousInputCommand() {
		previousInputState = null;
		return this;
	}
	
	public boolean handleSelectionAction() {
		boolean isResolved = false;
		final RankedItem<U> selectedElement = getCursoredOrDefaultElement();
		
		List<RankedItem<U>> tableEntries = getTableEntries();
		if (tableEntries.isEmpty()) return isResolved;
		
		if (setMultiResolvedAction != null)  {
			if (selectedEntries.size() == 0 && rowCursorIndex > -1) toggleSelectedState(tableEntries.get(rowCursorIndex));
			isResolved = true;
			setMultiResolvedAction.accept(selectedEntries.stream().map(rankedItem -> rankedItem.dataItem).collect(Collectors.toList()));
		}
		if (selectedElement != null && resolvedActionProvider != null) {
			isResolved = true;
			resolvedActionProvider.accept(selectedElement.dataItem);
		}
		
		return isResolved;
	}
	
	private RankedItem<U> getCursoredOrDefaultElement() {
		RankedItem<U> selectedElement = null;
		if (getSelectedEntries().size() == 0) {
			selectedElement = (RankedItem<U>) getCursorItem();
		} else if (getSelectedEntries().size() == 1) {
			selectedElement = selectedEntries.stream().findFirst().get();
		}
		return selectedElement;
	}

	public boolean handleContextSelectionAction(InternalContentProviderProxy<U> previousProvider) {
		boolean isResolved = false;
		final RankedItem<U> selectedElement = getCursoredOrDefaultElement();
		if (resolvedContextActionProvider != null) {
			resolvedContextActionProvider.accept(selectedElement.dataItem, previousProvider);
			previousInputState = null; // clear out when we execute command
			isResolved = true;
		}
		return isResolved;
	}	
	
	public boolean filterChanged(InputState inputState)	{
		if (previousInputState == null) {
			previousInputState = inputState;
			return true;
		}
		boolean filterChanged = !inputState.inputCommand.isFilterEqual(previousInputState.inputCommand);
		previousInputState = inputState;
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
	
	public InternalContentProviderProxy<U> sortDefault() {
		if (sortResolverFn != null) {
			sortResolverFn = null;
			return this;
		}
		
		BiFunction<U, Integer, String> contentFn = getKaviListColumns().getColumnOptions().get(1).getColumnContentFn();
		sortResolverFn = stream -> stream.sorted(Comparator.comparing(item -> contentFn.apply(item.dataItem, 0)));
		return this;
	}
}
