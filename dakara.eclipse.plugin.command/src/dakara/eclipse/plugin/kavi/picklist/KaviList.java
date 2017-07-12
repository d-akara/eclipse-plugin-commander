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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import dakara.eclipse.plugin.baseconverter.Base26AlphaBijectiveConverter;
import dakara.eclipse.plugin.stringscore.RankedItem;
import io.reactivex.subjects.PublishSubject;

public class KaviList<T> {
	private final KaviPickListDialog<T> rapidInputPickList;
	private Consumer<T> setResolvedAction;
	private Base26AlphaBijectiveConverter alphaColumnConverter = new Base26AlphaBijectiveConverter();
	
	private InputCommand previousInputCommand = null;
	private Consumer<List<RankedItem<T>>> changedAction = null;
	private BiConsumer<RankedItem<T>, InputCommand> fastSelectAction = null;
	private Map<String, KaviListContentProvider<T>> listContentProviders = new LinkedHashMap<>();
	
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

	public void setListContentChangedAction(Consumer<List<RankedItem<T>>> changedAction) {
		this.changedAction = changedAction;
	}
	
	public KaviListContentProvider<T> setListContentProvider(String name, Function<InputCommand, List<RankedItem<T>>> listContentProvider) {
		KaviListContentProvider<T> contentProvider = new KaviListContentProvider<>(this, name, listContentProvider);
		this.listContentProviders.put(name, contentProvider);
		return contentProvider;
	}
	
	public void setResolvedAction(Consumer<T> setResolvedAction) {
		this.setResolvedAction = setResolvedAction;
	}
	
	public void setShowAllWhenNoFilter(boolean showAllWhenNoFilter) {
		this.showAllWhenNoFilter = showAllWhenNoFilter;
	}

	public void requestRefresh(String filter) {
		subjectFilter.onNext(filter);
	}
	
	public void setFastSelectAction(BiConsumer<RankedItem<T>, InputCommand> fastSelectAction) {
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
				KaviListContentProvider<T> contentProvider = listContentProviders.get(currentContentProvider);
				List<RankedItem<T>> tableEntries = contentProvider.listContentProvider.apply(inputCommand);
				alphaColumnConverter = new Base26AlphaBijectiveConverter(tableEntries.size());
				display.asyncExec(() -> doTableRefresh(tableEntries));
			}
			
			display.asyncExec(() -> fastSelectItem(inputCommand));
		} catch (Throwable e) {
			// TODO how can we report error?
			e.printStackTrace();
		}
	}
	
	private void doTableRefresh(List<RankedItem<T>> tableEntries) {
		if (tableEntries == null) return;
		getCurrentContentProvider().setTableEntries(tableEntries);
		changedAction.accept(tableEntries);
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
		List<ColumnOptions<T>> columnOptions = getCurrentContentProvider().kaviListColumns.getColumnOptions();
		showOrHideFastSelectColumn(inputCommand, columnOptions);
		
		if ((inputCommand.fastSelectIndex != null) && (inputCommand.fastSelectIndex.length() == alphaColumnConverter.getNumberOfCharacters())){
			int itemIndex = alphaColumnConverter.toNumeric(inputCommand.fastSelectIndex) - 1;
			
			if (inputCommand.multiSelect) {
				if (table.isSelected(itemIndex)) table.deselect(itemIndex);
				else table.select(itemIndex);
			} else {
				table.setSelection(itemIndex);
				table.getDisplay().asyncExec(this::handleSelection);
			}
			
			if (fastSelectAction != null) fastSelectAction.accept((RankedItem<T>) table.getItem(itemIndex).getData(), inputCommand);
		}
	}

	private void showOrHideFastSelectColumn(final InputCommand inputCommand, List<ColumnOptions<T>> columnOptions) {
		final boolean isFastSelectShowing = columnOptions.get(0).width() > 0;
		// show fast select index if we are typing a fast select expression
		if ((inputCommand.fastSelect && !isFastSelectShowing)) {
			int columnWidth = averageCharacterWidth(columnOptions.get(0).getFont()) * alphaColumnConverter.getNumberOfCharacters() + 5;
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
		
		tableViewer.setContentProvider((ILazyContentProvider) o -> {
			tableViewer.replace(getCurrentContentProvider().getTableEntries().get(o), o);
			display.asyncExec(() -> getCurrentContentProvider().restoreTableState(this));

		});
		table.addListener(SWT.Selection, event-> handleSelection());
		composite.getShell().addListener(SWT.Resize, event ->  autoAdjustColumnWidths(composite));
		
		subjectFilter.debounce(0, TimeUnit.MILLISECONDS).subscribe( filter -> handleRefresh(filter));
	}

	private void autoAdjustColumnWidths(Composite composite) {
		KaviListContentProvider<T> contentProvider = getCurrentContentProvider();
		if (contentProvider == null) return;
		
		int fixedTotalColumnWidth = contentProvider.kaviListColumns.totalFixedColumnWidth();
		KaviListColumns<T> kaviListColumns = contentProvider.kaviListColumns;
		if (kaviListColumns.getColumnOptions().size() > 1) {
			int remainingWidth = composite.getShell().getSize().x - 25 - fixedTotalColumnWidth;
			for (ColumnOptions<T> options : kaviListColumns.getColumnOptions()) {
				int percentWidth = options.widthPercent();
				if (percentWidth > 0)
					options.width((int) (remainingWidth * (percentWidth / 100f)));
			}
		}
	}
	
	public KaviListContentProvider<T> getCurrentContentProvider() {
		return listContentProviders.get(currentContentProvider);
	}

	@SuppressWarnings("unchecked")
	private void handleSelection() {
		RankedItem<T> selectedElement = null;
		if (table.getSelectionCount() == 1) {
			selectedElement = (RankedItem<T>) table.getSelection()[0].getData();
		}
		
		KaviListContentProvider<T> listContentProvider = getCurrentContentProvider();
		
		List<RankedItem<T>> tableEntries = listContentProvider.getTableEntries();
		// TODO temp work around until we decide how to auto select
		// get first item in the list
		if ((selectedElement == null) && (tableEntries.size() > 0)) selectedElement = tableEntries.get(0);
		if (selectedElement != null) {
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
					// TODO wrap around
					int itemsInViewPort = numberOfItemsVisible(tableViewer.getTable());
					tableViewer.getTable().setTopIndex(tableViewer.getTable().getTopIndex()+itemsInViewPort);
				}
				if (isKeys(SWT.CTRL, 'k', e)) {
					e.doit = false;
					int itemsInViewPort = numberOfItemsVisible(tableViewer.getTable());
					// TODO wrap around
					tableViewer.getTable().setTopIndex(tableViewer.getTable().getTopIndex()-itemsInViewPort);
				}
				switch (e.keyCode) {
				case SWT.ARROW_DOWN:
					moveSelectionDown();
					break;
				case SWT.ARROW_UP:
					moveSelectionUp();
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
	
	private void moveSelectionDown() {
		int index = table.getSelectionIndex();
		if (index == table.getItemCount() - 1) {
			table.setSelection(-1);
		}
		else if (table.getItemCount() > index + 1) {
			table.setSelection(index + 1);
		}
	}
	
	private void moveSelectionUp() {
		int index = table.getSelectionIndex();
		if (index >= 0) {
			table.setSelection(index - 1);
		} else {
			table.setSelection(table.getItemCount() - 1);
		}
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
		if (currentContentProvider != null)
			getCurrentContentProvider().storeCurrentTableState(this);
		currentContentProvider = mode;
		Composite composite = table.getParent();
		composite.getShell().setRedraw(false);
		getCurrentContentProvider().installProvider();
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
	
	public static class KaviListContentProvider<T> {
		private List<RankedItem<T>> tableEntries;
		private final Set<RankedItem<T>> selectedEntries = new HashSet<>();
		private boolean tableStateRestored = true;
		public final Function<InputCommand, List<RankedItem<T>>> listContentProvider; 
		public final String name;
		private KaviListColumns<T> kaviListColumns;
		public KaviListContentProvider(KaviList<T> kaviList, String name, Function<InputCommand, List<RankedItem<T>>> listContentProvider) {
			this.name = name;
			this.listContentProvider = listContentProvider;
			
			KaviListColumns<T> kaviListColumns = new KaviListColumns<T>(kaviList.tableViewer);
			kaviListColumns.addColumn("fastSelect", (item, rowIndex) -> kaviList.alphaColumnConverter.toAlpha(rowIndex + 1)).width(0).searchable(false).backgroundColor(242, 215, 135).setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
			this.kaviListColumns = kaviListColumns;
		}
		
		public ColumnOptions<T> addColumn(String columnId, Function<T, String> columnContentFn) {
			return kaviListColumns.addColumn(columnId, (item, rowIndex) -> columnContentFn.apply(item));
		}	
		
		private void installProvider() {
			kaviListColumns.reset().installColumnsIntoTable();
		}
		
		private KaviListContentProvider<T> setTableEntries(List<RankedItem<T>> tableEntries) {
			this.tableEntries = tableEntries;
			return this;
		}
		
		private List<RankedItem<T>> getTableEntries() {
			return this.tableEntries;
		}
		
		@SuppressWarnings("unchecked")
		private KaviListContentProvider<T> storeCurrentTableState(KaviList<T> kaviList) {
			tableStateRestored = false;
			selectedEntries.clear();
			for (TableItem tableItem : kaviList.table.getSelection()) {
				selectedEntries.add((RankedItem<T>) tableItem.getData());
			}
			
			return this;
		}
		
		private KaviListContentProvider<T> restoreTableState(KaviList<T> kaviList) {
			if (tableStateRestored || selectedEntries.size() == 0) return this;

			int indexCount = 0;
			for (TableItem tableItem : kaviList.table.getItems()) {
				if (selectedEntries.contains(tableItem.getData()))
					kaviList.table.select(indexCount);
				indexCount++;
			}
			tableStateRestored = true;
			return this;
		}	
	}
}
