package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import dakara.eclipse.plugin.baseconverter.Base26AlphaBijectiveConverter;
import dakara.eclipse.plugin.log.EclipsePluginLogger;
import dakara.eclipse.plugin.stringscore.RankedItem;
import io.reactivex.subjects.PublishSubject;

public class KaviList<T> {
	private EclipsePluginLogger logger = new EclipsePluginLogger("dakara.eclipse.commander.plugin");
	
	private final KaviPickListDialog<T> rapidInputPickList;
	private Consumer<T> setResolvedAction;
	private Consumer<List<T>> setMultiResolvedAction;
	private Base26AlphaBijectiveConverter alphaColumnConverter = new Base26AlphaBijectiveConverter();
	
	private InputCommand previousInputCommand = null;
	private BiConsumer<List<RankedItem<T>>, Set<RankedItem<T>>> changedAction = null;
	private BiConsumer<Set<RankedItem<T>>, InputCommand> fastSelectAction = null;
	private Map<String, InternalContentProviderProxy<T>> listContentProviders = new LinkedHashMap<>();
	
	private String currentContentProvider;
	private boolean showAllWhenNoFilter = true;

	private TableViewer tableViewer;
	private Table table;
	private Display display;
	private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());
	
	private PublishSubject<String> subjectFilter = PublishSubject.create();

	public KaviList(KaviPickListDialog<T> rapidInputPickList) {
		this.rapidInputPickList = rapidInputPickList;
	}

	public void setListContentChangedAction(BiConsumer<List<RankedItem<T>>, Set<RankedItem<T>>> changedAction) {
		this.changedAction = changedAction;
	}
	
	public InternalContentProviderProxy<T> setListContentProvider(String name, Function<InputCommand, List<RankedItem<T>>> listContentProvider) {
		InternalContentProviderProxy<T> contentProvider = new InternalContentProviderProxy<>(this, name, listContentProvider);
		this.listContentProviders.put(name, contentProvider);
		return contentProvider;
	}
	
	public void setResolvedAction(Consumer<T> setResolvedAction) {
		this.setResolvedAction = setResolvedAction;
	}
	
	public void setMultiResolvedAction(Consumer<List<T>> setResolvedAction) {
		this.setMultiResolvedAction = setResolvedAction;
	}
	
	public void setShowAllWhenNoFilter(boolean showAllWhenNoFilter) {
		this.showAllWhenNoFilter = showAllWhenNoFilter;
	}

	public void requestRefresh(String filter) {
		subjectFilter.onNext(filter);
	}
	
	public void setFastSelectAction(BiConsumer<Set<RankedItem<T>>, InputCommand> fastSelectAction) {
		this.fastSelectAction = fastSelectAction;
	}

	/*
	 * This will be executed on rxJava thread due to debouncing
	 * We will handle the computations of filtering on the background thread
	 * and must let SWT handle the table updates on the UI thread.
	 */
	private void handleRefresh(String filter) {
		try {
			if (table == null) return;
			
			if (!showAllWhenNoFilter && filter.length() == 0) {
				previousInputCommand = null;
				display.asyncExec(() -> doTableRefresh(new ArrayList<>()));
				return;
			}
			
			final InputCommand inputCommand = InputCommand.parse(filter, currentContentMode());
			if (filterChanged(inputCommand)) {
				InternalContentProviderProxy<T> contentProvider = listContentProviders.get(currentContentProvider);
				List<RankedItem<T>> tableEntries = contentProvider.listContentProvider.apply(inputCommand);
				alphaColumnConverter = new Base26AlphaBijectiveConverter(tableEntries.size());
				display.asyncExec(() -> doTableRefresh(tableEntries));
			}
			
			display.asyncExec(() -> fastSelectItem(inputCommand));
		} catch (Throwable e) {
			logger.info("Problem occurred refreshing content with filter '" +filter+ "'", e);
		}
	}
	
	private void doTableRefresh(List<RankedItem<T>> tableEntries) {
		// TODO - review how many times we call refresh - performance
		if (tableEntries == null) return;
		contentProvider().setTableEntries(tableEntries);
		changedAction.accept(tableEntries, contentProvider().getSelectedEntries());
		table.removeAll();
		table.setItemCount(tableEntries.size());	
	}
	
	private boolean filterChanged(InputCommand inputCommand)	{
		if (previousInputCommand == null) {
			previousInputCommand = inputCommand;
			return true;
		}
		boolean filterChanged = !inputCommand.isFilterEqual(previousInputCommand);
		filterChanged |= !inputCommand.contentMode.equals(previousInputCommand.contentMode);
		previousInputCommand = inputCommand;
		return filterChanged;
	}

	@SuppressWarnings("unchecked")
	private void fastSelectItem(final InputCommand inputCommand) {
		List<ColumnOptions<T>> columnOptions = contentProvider().kaviListColumns.getColumnOptions();
		showOrHideFastSelectColumn(inputCommand, columnOptions);
		
		if ((inputCommand.fastSelectIndex != null) && (inputCommand.fastSelectIndex.length() == alphaColumnConverter.getNumberOfCharacters())){
			int rowIndex = alphaColumnConverter.toNumeric(inputCommand.fastSelectIndex) - 1;
			
			if (inputCommand.multiSelect && inputCommand.selectRange) {
				contentProvider().selectRange((RankedItem<T>) table.getItem(rowIndex).getData());
				tableViewer.refresh();
			} else if (inputCommand.multiSelect) {
				contentProvider().toggleSelectedState((RankedItem<T>) table.getItem(rowIndex).getData());
				tableViewer.refresh();
			} else {
				contentProvider().toggleSelectedState((RankedItem<T>) table.getItem(rowIndex).getData());
				table.getDisplay().asyncExec(this::handleSelection);
			}
			
			if (fastSelectAction != null) fastSelectAction.accept(contentProvider().getSelectedEntries(), inputCommand);
		} else if (inputCommand.inverseSelection) {
			contentProvider().inverseSelectedState();
			tableViewer.refresh();
			if (fastSelectAction != null) fastSelectAction.accept(contentProvider().getSelectedEntries(), inputCommand);
		} else if (inputCommand.selectAll) {
			contentProvider().toggleSelectedState();
			tableViewer.refresh();
			if (fastSelectAction != null) fastSelectAction.accept(contentProvider().getSelectedEntries(), inputCommand);
		}
	}

	private void showOrHideFastSelectColumn(final InputCommand inputCommand, List<ColumnOptions<T>> columnOptions) {
		final boolean isFastSelectShowing = columnOptions.get(0).width() > 0;
		// show fast select index if we are typing a fast select expression
		if ((inputCommand.fastSelect && !isFastSelectShowing)) {
			int columnWidth = averageCharacterWidth(columnOptions.get(0).getFont()) * alphaColumnConverter.getNumberOfCharacters() + getAdjustmentForFastSelectColumn();
			columnOptions.get(0).width(columnWidth);
			columnOptions.get(1).changeWidth(-columnWidth + 1);
		} else if (!inputCommand.fastSelect && isFastSelectShowing) {
			// change column 1 the amount of column 0
			columnOptions.get(1).changeWidth(columnOptions.get(0).width() - 1);
			columnOptions.get(0).width(0);
		}
	}
	
	private int averageCharacterWidth(Font font) {
		int width;
	    GC gc = new GC(Display.getDefault());
	    gc.setFont(font);
	    FontMetrics fontMetrics = gc.getFontMetrics();
		width = fontMetrics.getAverageCharWidth();
		gc.dispose();
		return width;
	}

    private int numberOfItemsVisible(Table table) {
		Rectangle rectange = table.getClientArea();
		int itemHeight = table.getItemHeight();
		int headerHeight = table.getHeaderHeight();
		return (rectange.height - headerHeight ) / itemHeight;
    }

	public void initialize(Composite composite, int defaultOrientation) {
		display = composite.getDisplay();
		composite.addDisposeListener((DisposeListener) this::dispose);
		
		tableViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED );
		table = tableViewer.getTable();
		
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        tableViewer.getControl().setLayoutData(gridData);
		
		tableViewer.setContentProvider((ILazyContentProvider) rowIndex -> tableViewer.replace(contentProvider().getTableEntries().get(rowIndex), rowIndex));
		table.addListener(SWT.Selection, event-> handleSelection());
		composite.getShell().addListener(SWT.Resize, event ->  autoAdjustColumnWidths(composite));
		
		subjectFilter.debounce(0, TimeUnit.MILLISECONDS).subscribe( filter -> handleRefresh(filter));
	}

	private void autoAdjustColumnWidths(Composite composite) {
		InternalContentProviderProxy<T> contentProvider = contentProvider();
		if (contentProvider == null) return;
		final int fixedTotalColumnWidth = contentProvider.kaviListColumns.totalFixedColumnWidth();
		KaviListColumns<T> kaviListColumns = contentProvider.kaviListColumns;
		if (kaviListColumns.getColumnOptions().size() > 1) {
			int remainingWidth = composite.getShell().getSize().x - getAdjustmentForTableWidth() - fixedTotalColumnWidth;
			for (ColumnOptions<T> options : kaviListColumns.getColumnOptions()) {
				int percentWidth = options.widthPercent();
				if (percentWidth > 0)
					options.width((int) (remainingWidth * (percentWidth / 100f)));
			}
		}
	}
	
	private int getAdjustmentForTableWidth() {
		return SWT.getPlatform().equals("win32") ? 40 : 25;
	}
	
	private int getAdjustmentForFastSelectColumn() {
		return SWT.getPlatform().equals("win32") ? 9 : 4;
	}
	
	private int getAdjustmentForRowCountVisible() {
		return SWT.getPlatform().equals("win32") ? 0 : 1;
	}
	
	public InternalContentProviderProxy<T> contentProvider() {
		return listContentProviders.get(currentContentProvider);
	}

	private void handleSelection() {
		RankedItem<T> selectedElement = null;
		if (contentProvider().getSelectedEntries().size() <= 1) {
			selectedElement = (RankedItem<T>) contentProvider().getCursorItem();
		}
		
		InternalContentProviderProxy<T> listContentProvider = contentProvider();
		
		List<RankedItem<T>> tableEntries = listContentProvider.getTableEntries();
		if (tableEntries.isEmpty()) return;
		
		if (setMultiResolvedAction != null)  {
			if (listContentProvider.selectedEntries.size() == 0) listContentProvider.toggleSelectedState(listContentProvider.tableEntries.get(0));
			close();
			setMultiResolvedAction.accept(listContentProvider.selectedEntries.stream().map(rankedItem -> rankedItem.dataItem).collect(Collectors.toList()));
		}
		// TODO temp work around until we decide how to auto select
		// get first item in the list
		if ((selectedElement == null) && (tableEntries.size() > 0)) selectedElement = tableEntries.get(0);
		if (selectedElement != null && setResolvedAction != null) {
			close();
			setResolvedAction.accept(selectedElement.dataItem);
		}
	}

	public void bindInputField(Text filterText) {
		// TODO - create separate key binding manager
		// check for keys being held down
		// possibly tab toggle for command mode
		filterText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (isKeys(SWT.CTRL, 'j', e)) {
					e.doit = false; // prevent beeping
					scrollPageDown();
				}
				if (isKeys(SWT.CTRL, 'k', e)) {
					e.doit = false;
					scrollPageUp();
				}
				if (isKeys(SWT.CTRL, 'h', e)) {
					e.doit = false;
					filterText.setSelection(0);
				}
				if (isKeys(SWT.CTRL, 'l', e)) {
					e.doit = false;
					filterText.setSelection(filterText.getText().length());
				}
				if (isKeys(SWT.CTRL, 'x', e)) {
					e.doit = false;
					filterText.setText("");
				}				
				switch (e.keyCode) {
				case SWT.ARROW_DOWN:
					e.doit = false;
					contentProvider().moveCursorDown().getCursorItem();
					tableViewer.getTable().setTopIndex(contentProvider().getCursorIndex());
					tableViewer.refresh();
					break;
				case SWT.ARROW_UP:
					e.doit = false;
					contentProvider().moveCursorUp().getCursorItem();
					tableViewer.getTable().setTopIndex(contentProvider().getCursorIndex());
					tableViewer.refresh();
					break;
				case SWT.CR:
					handleSelection();
					break;
				case SWT.ESC:
					close();
					break;
				case SWT.TAB:
					nextContentMode();
					table.getParent().getShell().setRedraw(false);
					handleRefresh(( (Text) e.widget).getText());
					display.asyncExec(() -> table.getParent().getShell().setRedraw(true));
					break;
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});
		
		// capture all keys in the input.  So we can capture TAB etc
		filterText.addTraverseListener((TraverseListener) event -> event.doit = false);
		
		filterText.addModifyListener((ModifyListener) event -> requestRefresh(((Text) event.widget).getText()));
	}
	
	private boolean isKeys(int modifier, int keyCode, KeyEvent event) {
		return ((event.stateMask & modifier) != 0) && (event.keyCode == keyCode);
	}
	
	private void nextContentMode() {
		String[] keys = listContentProviders.keySet().toArray(new String[] {});
		for (int keyIndex = 0; keyIndex < keys.length; keyIndex++) {
			String key = keys[keyIndex];
			if (key.equals(currentContentProvider)) {
				if (keyIndex < keys.length - 1) setCurrentProvider(keys[keyIndex + 1]);
				else setCurrentProvider(keys[0]);
				break;
			}
		}
	}
	
	public String currentContentMode() {
		return currentContentProvider;
	}
	
	public void setCurrentProvider(String mode) {
		if (currentContentProvider != null && currentContentProvider.equals(mode)) return;
//		if (currentContentProvider != null)
//			getCurrentContentProvider().storeCurrentTableState(this);
		currentContentProvider = mode;
		Composite composite = table.getParent();
		composite.getShell().setRedraw(false);
		contentProvider().installProvider();
		autoAdjustColumnWidths(composite);
		
		// do this async to prevent timing related flicker
		display.asyncExec(() -> composite.getShell().setRedraw(true));
	}
	
	protected void close() {
		rapidInputPickList.hide();
	}
	
	private void dispose(DisposeEvent e) {
		if (resourceManager != null) {
			resourceManager.dispose();
			resourceManager = null;
		}
	}
	
	private void scrollPageDown() {
		int itemsInViewPort = numberOfItemsVisible(tableViewer.getTable());
		int topIndex = tableViewer.getTable().getTopIndex() + itemsInViewPort;
		if (topIndex == tableViewer.getTable().getItemCount() - getAdjustmentForRowCountVisible()) topIndex = 0;
		tableViewer.getTable().setTopIndex(topIndex);
	}

	private void scrollPageUp() {
		int itemsInViewPort = numberOfItemsVisible(tableViewer.getTable());
		int topIndex = tableViewer.getTable().getTopIndex();
		if (topIndex == 0) topIndex = tableViewer.getTable().getItemCount() - itemsInViewPort;
		else topIndex -= itemsInViewPort;
		tableViewer.getTable().setTopIndex(topIndex);
	}

	public static class InternalContentProviderProxy<T> {
		public enum RowState {
			SELECTED(1),
			CURSOR(2);
			
			public final int value;
			RowState(int value) {this.value = value;}
		};
		
		private List<RankedItem<T>> tableEntries;
		private final Set<RankedItem<T>> selectedEntries = new HashSet<>();
		private int rowCursorIndex = -1;
		public final Function<InputCommand, List<RankedItem<T>>> listContentProvider; 
		public final String name;
		private KaviListColumns<T> kaviListColumns;
		public InternalContentProviderProxy(KaviList<T> kaviList, String name, Function<InputCommand, List<RankedItem<T>>> listContentProvider) {
			this.name = name;
			this.listContentProvider = listContentProvider;
			
			KaviListColumns<T> kaviListColumns = new KaviListColumns<T>(kaviList.tableViewer, this::isSelected);
			kaviListColumns.addColumn("fastSelect", (item, rowIndex) -> kaviList.alphaColumnConverter.toAlpha(rowIndex + 1)).width(0).searchable(false).backgroundColor(242, 215, 135).setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT)).setEnableBackgroundSelection(false);;
			this.kaviListColumns = kaviListColumns;
		}
		
		public ColumnOptions<T> addColumn(String columnId, Function<T, String> columnContentFn) {
			return kaviListColumns.addColumn(columnId, (item, rowIndex) -> columnContentFn.apply(item));
		}	
		
		private void installProvider() {
			kaviListColumns.reset().installColumnsIntoTable();
		}
		
		private InternalContentProviderProxy<T> setTableEntries(List<RankedItem<T>> tableEntries) {
			this.tableEntries = tableEntries;
			return this;
		}
		
		private List<RankedItem<T>> getTableEntries() {
			return this.tableEntries;
		}
		
		private InternalContentProviderProxy<T> moveCursorDown() {
			if (rowCursorIndex == tableEntries.size() - 1) {
				rowCursorIndex = -1;
			}
			else if (tableEntries.size() > rowCursorIndex + 1) {
				rowCursorIndex++;
			}
			
			return this;
		}
		
		private InternalContentProviderProxy<T> moveCursorUp() {
			if (rowCursorIndex >= 0) {
				rowCursorIndex--;
			} else {
				rowCursorIndex = tableEntries.size() - 1;
			}
			
			return this;
		}
		
		private RankedItem<T> getCursorItem() {
			if (rowCursorIndex < 0) return null;
			return tableEntries.get(rowCursorIndex);
		}
		
		private int getCursorIndex() {
			return rowCursorIndex;
		}
		
		private Set<RankedItem<T>> getSelectedEntries() {
			return selectedEntries;
		}
		
		private int getRowIndex(RankedItem<T> rankedItem) {
			return tableEntries.indexOf(rankedItem);
		}
		
		private InternalContentProviderProxy<T> toggleSelectedState(RankedItem<T> item) {
			if (selectedEntries.contains(item)) selectedEntries.remove(item);
			else selectedEntries.add(item);
			rowCursorIndex = tableEntries.indexOf(item);
			return this;
		}
		
		private InternalContentProviderProxy<T> setSelectedState(RankedItem<T> item, boolean selected) {
			if (selected) selectedEntries.add(item);
			else selectedEntries.remove(item);
			return this;
		}
		
		private InternalContentProviderProxy<T> toggleSelectedState() {
			if (selectedEntries.size() == 0) {
				selectedEntries.addAll(tableEntries);
			} else {
				selectedEntries.clear();
			}
			rowCursorIndex = -1;
			
			return this;
		}
		
		private InternalContentProviderProxy<T> selectRange(RankedItem<T> item) {
			if (rowCursorIndex < 0) return this;
			
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
		
		private InternalContentProviderProxy<T> inverseSelectedState() {
			for (RankedItem<T> rankedItem : tableEntries) {
				if (selectedEntries.contains(rankedItem)) {
					selectedEntries.remove(rankedItem);
				} else {
					selectedEntries.add(rankedItem);
				}
			}
			return this;
		}
		
		private int isSelected(RankedItem<T> item) {
			int state = 0;
			if (selectedEntries.contains(item)) state |= RowState.SELECTED.value;
			if (rowCursorIndex > -1 && tableEntries.indexOf(item) == rowCursorIndex) state |= RowState.CURSOR.value;
			
			return state;
		}
	}
}
