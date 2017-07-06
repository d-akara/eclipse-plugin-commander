package dakara.eclipse.plugin.kavi.picklist;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

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
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import dakara.eclipse.plugin.baseconverter.Base26AlphaBijectiveConverter;
import dakara.eclipse.plugin.stringscore.RankedItem;

public class KaviList<T> {
	private final KaviPickListDialog<T> rapidInputPickList;
	private List<RankedItem<T>> tableEntries;
	private Consumer<T> handleSelectFn;
	private Base26AlphaBijectiveConverter alphaColumnConverter = new Base26AlphaBijectiveConverter();
	
	private InputCommand previousInputCommand = null;
	private Consumer<List<RankedItem<T>>> changedAction = null;
	private Map<String, KaviListContentProvider<T>> listContentProviders = new LinkedHashMap<>();
	
	private String currentContentProvider;
	private boolean showAllWhenNoFilter = true;

	private TableViewer tableViewer;
	private Table table;
	private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

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
	
	public void setSelectionAction(Consumer<T> handleSelectFn) {
		this.handleSelectFn = handleSelectFn;
	}
	
	public void setShowAllWhenNoFilter(boolean showAllWhenNoFilter) {
		this.showAllWhenNoFilter = showAllWhenNoFilter;
	}

	public void refresh(String filter) {
		if (table == null) return;
		
		if (!showAllWhenNoFilter && filter.length() == 0) {
			table.removeAll();
			table.setItemCount(0);
			if (tableEntries != null) tableEntries.clear();
			previousInputCommand = null;
			return;
		}
		
		final InputCommand inputCommand = InputCommand.parse(filter, currentContentMode() ).get(0);
		if (filterChanged(inputCommand)) {
			KaviListContentProvider<T> contentProvider = listContentProviders.get(currentContentProvider);
			tableEntries = contentProvider.listContentProvider.apply(inputCommand);
			alphaColumnConverter = new Base26AlphaBijectiveConverter(tableEntries.size());
			table.removeAll();
			table.setItemCount(tableEntries.size());
			changedAction.accept(tableEntries);
		}
		
		fastSelectItem(inputCommand);		
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

	private void fastSelectItem(final InputCommand inputCommand) {
		List<ColumnOptions<T>> columnOptions = getCurrentContentProvider().kaviListColumns.getColumnOptions();
		final boolean isFastSelectShowing = columnOptions.get(0).width() > 0;
		// show fast select index if we are typing a fast select expression
		if ((inputCommand.fastSelect && !isFastSelectShowing)) {
			int columnWidth = averageCharacterWidth(columnOptions.get(0).getFont()) * alphaColumnConverter.getNumberOfCharacters() + 5;
			columnOptions.get(0).width(columnWidth);
			columnOptions.get(1).changeWidth(-columnWidth + 1);
			// force redraw
			table.removeAll();
			table.setItemCount(tableEntries.size());
		} else if (!inputCommand.fastSelect && isFastSelectShowing) {
			// change column 1 the amount of column 0
			columnOptions.get(1).changeWidth(columnOptions.get(0).width() - 1);
			columnOptions.get(0).width(0);
		}
		
		if ((inputCommand.fastSelectIndex != null) && (inputCommand.fastSelectIndex.length() == alphaColumnConverter.getNumberOfCharacters())){
			table.setSelection(alphaColumnConverter.toNumeric(inputCommand.fastSelectIndex) - 1);
			table.getDisplay().asyncExec(this::handleSelection);
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
	
    public int getTotalColumnWidth() {
    		return Stream.of(table.getColumns()).map(column -> column.getWidth()).reduce((width1, width2) -> width1 + width2).orElse(400);
    }
    
    private int numberOfItemsVisible(Table table) {
		Rectangle rectange = table.getClientArea();
		int itemHeight = table.getItemHeight();
		int headerHeight = table.getHeaderHeight();
		return (rectange.height - headerHeight ) / itemHeight;
    }

	public void initialize(Composite composite, int defaultOrientation) {
		composite.addDisposeListener((DisposeListener) this::dispose);
		
		tableViewer = new TableViewer(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.NO_BACKGROUND | SWT.DOUBLE_BUFFERED );
		table = tableViewer.getTable();
		
        GridData gridData = new GridData();
        gridData.verticalAlignment = GridData.FILL;
        gridData.horizontalSpan = 2;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        tableViewer.getControl().setLayoutData(gridData);
		
		tableViewer.setContentProvider((ILazyContentProvider) o -> tableViewer.replace(tableEntries.get(o), o));

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent event) {
				if (!table.equals(event.getSource())) return;
				if (event.button != 1) return;
				if (table.getSelectionCount() < 1) return;

				if (isMouseEventOverSelection(event))
					handleSelection();
			}
		});

		table.addListener(SWT.Selection, event-> handleSelection());
		
		listContentProviders.values().forEach(contentProvider -> {
			KaviListColumns<T> kaviListColumns = new KaviListColumns<T>(tableViewer);
			kaviListColumns.addColumn("fastSelect", (item, rowIndex) -> alphaColumnConverter.toAlpha(rowIndex + 1)).searchable(false).backgroundColor(242, 215, 135).setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
			contentProvider.kaviListColumns = kaviListColumns;
		});
        
		composite.getShell().addListener(SWT.Resize, event -> {
			autoAdjustColumnWidths(composite);
		});
	}

	private void autoAdjustColumnWidths(Composite composite) {
		KaviListContentProvider<T> contentProvider = getCurrentContentProvider();
		if (contentProvider == null) return;
		
		KaviListColumns<T> kaviListColumns = contentProvider.kaviListColumns;
		if (kaviListColumns.getColumnOptions().size() > 1) {
			int widthRightOfFirstColumn = getTotalColumnWidth() - kaviListColumns.getColumnOptions().get(1).width() + 25;
			kaviListColumns.getColumnOptions().get(1).width(composite.getShell().getSize().x - widthRightOfFirstColumn);	// TODO compute size
		}
	}
	
	
	public KaviListContentProvider<T> getCurrentContentProvider() {
		return listContentProviders.get(currentContentProvider);
	}
	
	private boolean isMouseEventOverSelection(MouseEvent event) {
		TableItem itemUnderMouse = table.getItem(new Point(event.x, event.y));
		TableItem itemSelection = table.getSelection()[0];
		return itemSelection.equals(itemUnderMouse);
	}

	@SuppressWarnings("unchecked")
	private void handleSelection() {
		RankedItem<T> selectedElement = null;
		if (table.getSelectionCount() == 1) {
			selectedElement = (RankedItem<T>) table.getSelection()[0].getData();
		}
		// TODO temp work around until we decide how to auto select
		// get first item in the list
		if ((selectedElement == null) && (tableEntries.size() > 0)) selectedElement = tableEntries.get(0);
		if (selectedElement != null) {
			close();
			handleSelectFn.accept(selectedElement.dataItem);
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
					table.getDisplay().asyncExec(() -> refresh(( (Text) e.widget).getText()));
					break;
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});
		
		// capture all keys in the input.  So we can capture TAB etc
		filterText.addTraverseListener((TraverseListener) event -> event.doit = false);
		
		filterText.addModifyListener((ModifyListener) event -> refresh(((Text) event.widget).getText()));
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
		currentContentProvider = mode;
		Composite composite = table.getParent();
		composite.getShell().setRedraw(false);
		getCurrentContentProvider().setTableColumnsForProvider();
		autoAdjustColumnWidths(composite);
		table.getDisplay().asyncExec(() -> composite.getShell().setRedraw(true));
		//composite.getShell().setRedraw(true);
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
		public final Function<InputCommand, List<RankedItem<T>>> listContentProvider; 
		public final String name;
		private KaviListColumns<T> kaviListColumns;
		public KaviListContentProvider(KaviList<T> kaviList, String name, Function<InputCommand, List<RankedItem<T>>> listContentProvider) {
			this.name = name;
			this.listContentProvider = listContentProvider;
			
			KaviListColumns<T> kaviListColumns = new KaviListColumns<T>(kaviList.tableViewer);
			kaviListColumns.addColumn("fastSelect", (item, rowIndex) -> kaviList.alphaColumnConverter.toAlpha(rowIndex + 1)).searchable(false).backgroundColor(242, 215, 135).setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
			this.kaviListColumns = kaviListColumns;
		}
		
		public ColumnOptions<T> addColumn(String columnId, Function<T, String> columnContentFn) {
			return kaviListColumns.addColumn(columnId, (item, rowIndex) -> columnContentFn.apply(item));
		}	
		
		private void setTableColumnsForProvider() {
			kaviListColumns.reset().installColumnsIntoTable();
		}
	}
}
