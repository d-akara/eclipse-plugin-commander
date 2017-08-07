package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
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
import org.eclipse.swt.widgets.Text;

import dakara.eclipse.plugin.baseconverter.Base26AlphaBijectiveConverter;
import dakara.eclipse.plugin.command.Constants;
import dakara.eclipse.plugin.log.EclipsePluginLogger;
import dakara.eclipse.plugin.stringscore.RankedItem;
import io.reactivex.subjects.PublishSubject;

public class KaviList<T> {
	private EclipsePluginLogger logger = new EclipsePluginLogger(Constants.BUNDLE_ID);
	
	private final KaviPickListDialog<T> rapidInputPickList;
	private Base26AlphaBijectiveConverter alphaColumnConverter = new Base26AlphaBijectiveConverter();
	
	private InternalContentProviderProxy<T> previousProvider = null;
	private InternalContentProviderProxy<T> lastRefreshWithProvider = null;
	private BiConsumer<List<RankedItem<T>>, Set<RankedItem<T>>> changedAction = null;
	private BiConsumer<Set<RankedItem<T>>, InputCommand> fastSelectAction = null;
	@SuppressWarnings("rawtypes")
	private Map<String, InternalContentProviderProxy> listContentProviders = new LinkedHashMap<>();
	
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
	
	public <U> InternalContentProviderProxy<U> setListContentProvider(String name, Function<InputState, List<RankedItem<U>>> listContentProvider) {
		InternalContentProviderProxy<U> contentProvider = new InternalContentProviderProxy<U>(this, name, listContentProvider);
		KaviListColumns<U> kaviListColumns = new KaviListColumns<U>(tableViewer, contentProvider::isSelected);
		kaviListColumns.addColumn("fastSelect", (item, rowIndex) -> alphaColumnConverter.toAlpha(rowIndex + 1)).width(0).searchable(false)
					   .backgroundColor(242, 215, 135).setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT)).setEnableBackgroundSelection(false);	
		contentProvider.setKaviListColumns(kaviListColumns);
		
		this.listContentProviders.put(name, contentProvider);
		return contentProvider;
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
				contentProvider().clearPreviousInputCommand();
				display.asyncExec(() -> doTableRefresh(new ArrayList<>()));
				return;
			}
			
			final InputCommand inputCommand = InputCommand.parse(filter);
			if (contentProvider().filterChanged(inputCommand) | providerChanged()) {
				InternalContentProviderProxy<T> contentProvider = listContentProviders.get(currentContentProvider);
				InputState inputState = new InputState(inputCommand, contentProvider(), previousProvider);
				List<RankedItem<T>> tableEntries = contentProvider.updateTableEntries(inputState).getTableEntries();
				alphaColumnConverter = new Base26AlphaBijectiveConverter(tableEntries.size());
				display.asyncExec(() -> doTableRefresh(tableEntries));
			}
			
			display.asyncExec(() -> fastSelectItem(inputCommand));
		} catch (Throwable e) {
			logger.error("Problem occurred refreshing content with filter '" +filter+ "'", e);
		}
	}
	
	private void doTableRefresh(List<RankedItem<T>> tableEntries) {
		// TODO - review how many times we call refresh - performance
		if (tableEntries == null) return;
		//contentProvider().setTableEntries(tableEntries);
		changedAction.accept(contentProvider().getTableEntries(), contentProvider().getSelectedEntries());
		table.removeAll();
		table.setItemCount(contentProvider().getTableEntries().size());	
	}
	
	private boolean providerChanged() {
		boolean providerChanged = true;
		if (lastRefreshWithProvider == contentProvider()) providerChanged = false;
		
		lastRefreshWithProvider = contentProvider();
		return providerChanged;
	}

	@SuppressWarnings("unchecked")
	private void fastSelectItem(final InputCommand inputCommand) {
		List<ColumnOptions<T>> columnOptions = contentProvider().getKaviListColumns().getColumnOptions();
		showOrHideFastSelectColumn(inputCommand, columnOptions);
		
		if ((inputCommand.fastSelectIndex != null) && (inputCommand.fastSelectIndex.length() == alphaColumnConverter.getNumberOfCharacters())){
			int rowIndex = alphaColumnConverter.toNumeric(inputCommand.fastSelectIndex) - 1;
			
			if (inputCommand.multiSelect && inputCommand.selectRange) {
				contentProvider().selectRange(contentProvider().getTableEntries().get(rowIndex));
				tableViewer.refresh();
			} else if (inputCommand.multiSelect) {
				contentProvider().toggleSelectedState(contentProvider().getTableEntries().get(rowIndex));
				tableViewer.refresh();
			} else {
				contentProvider().toggleSelectedState(contentProvider().getTableEntries().get(rowIndex));
				table.getDisplay().asyncExec(this::handleSelection);
			}
			
			if (fastSelectAction != null) fastSelectAction.accept(contentProvider().getSelectedEntries(), inputCommand);
		} else if (inputCommand.inverseSelection) {
			contentProvider().inverseSelectedState();
			tableViewer.refresh();
			if (fastSelectAction != null) fastSelectAction.accept(contentProvider().getSelectedEntries(), inputCommand);
		} else if (inputCommand.selectAll) {
			contentProvider().toggleSelectedStateOfVisible();
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
		final int fixedTotalColumnWidth = contentProvider.getKaviListColumns().totalFixedColumnWidth();
		KaviListColumns<T> kaviListColumns = contentProvider.getKaviListColumns();
		if (kaviListColumns.getColumnOptions().size() > 1) {
			int remainingWidth = composite.getShell().getSize().x - getAdjustmentForTableWidth() - fixedTotalColumnWidth;
			for (ColumnOptions<T> options : kaviListColumns.getColumnOptions()) {
				int percentWidth = options.widthPercent();
				if (percentWidth > 0)
					options.width((int) (remainingWidth * (percentWidth / 100f)));
			}
		}
	}
	
	private int getAdjustmentForTableWidth() 		{return SWT.getPlatform().equals("win32") ? 40 : 25;}
	private int getAdjustmentForFastSelectColumn() 	{return SWT.getPlatform().equals("win32") ? 9  : 4;}
	private int getAdjustmentForRowCountVisible() 	{return SWT.getPlatform().equals("win32") ? 0  : 1;}
	
	public InternalContentProviderProxy<T> contentProvider() {
		return listContentProviders.get(currentContentProvider);
	}

	private void handleSelection() {
		if (contentProvider().handleSelectionAction()) close();
		if (contentProvider().handleContextSelectionAction(previousProvider)) setCurrentProvider(previousProvider.name);
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
				case ';':
					e.doit = false;
					toggleInternalCommands();
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
		if (keys.length == 0) return;
		
		int keyIndex = 0;
		while (keyIndex <= keys.length) {
			String key = keys[keyIndex++ % keys.length];
			if (key.equals(currentContentProvider)) {
				String providerName = keys[keyIndex % keys.length];
				
				if (providerName.equals("context")) providerName = keys[(keyIndex + 1) % keys.length]; // skip to next item
				setCurrentProvider(providerName);
				break;
			}
		}
	}
	
	public String currentContentMode() {
		return currentContentProvider;
	}
	
	public void toggleInternalCommands() {
		if (currentContentProvider.equals("context")) {
			setCurrentProvider(previousProvider.name);
		} else {
			setCurrentProvider("context");
		}
	}
	
	public void setCurrentProvider(String mode) {
		if (currentContentProvider != null && currentContentProvider.equals(mode)) return;
		if (!providerExists(mode)) return;
		
		previousProvider = contentProvider();
		currentContentProvider = mode;
		Composite composite = table.getParent();
		composite.getShell().setRedraw(false);
		if (contentProvider().installProvider(previousProvider)) {
			if (contentProvider().previousInputCommand() != null)
				rapidInputPickList.setFilterInputText(contentProvider().previousInputCommand().filterText);
			else
				rapidInputPickList.setFilterInputText("");
		}
		autoAdjustColumnWidths(composite);
		
		// do this async to prevent timing related flicker
		display.asyncExec(() -> composite.getShell().setRedraw(true));
	}
	
	private boolean providerExists(String mode) {
		if (listContentProviders.containsKey(mode)) return true;
		else {
			logger.warn("Attempt to set provider `" + mode + "` does not exist");
			return false;
		}
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


}
