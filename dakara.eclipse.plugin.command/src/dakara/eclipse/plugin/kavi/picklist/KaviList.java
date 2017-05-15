package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import dakara.eclipse.plugin.baseconverter.Base26AlphaBijectiveConverter;
import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class KaviList<T> {
	private final KaviPickListDialog<T> rapidInputPickList;
	private List<KaviListItem<T>> tableEntries;
	private Function<InputCommand, List<T>> listContentProvider;
	private Consumer<T> handleSelectFn;
	private BiFunction<String, String, Score> rankingStrategy;
	private Function<T, String> sortFieldResolver; 
	private List<ColumnOptions<T>> columnOptions = new ArrayList<>();
	private Base26AlphaBijectiveConverter alphaColumnConverter = new Base26AlphaBijectiveConverter();

	private TableViewer tableViewer;
	private Table table;
	private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

	public KaviList(KaviPickListDialog<T> rapidInputPickList) {
		this.rapidInputPickList = rapidInputPickList;
	}

	public void setListContentProvider(Function<InputCommand, List<T>> listContentProvider) {
		this.listContentProvider = listContentProvider;
	}
	
	public ColumnOptions<T> addColumn(Function<T, String> columnContentFn) {
		return addColumn((item, columnIndex) -> columnContentFn.apply(item));
	}
	
	public ColumnOptions<T> addColumn(BiFunction<T, Integer, String> columnContentFn) {
		final ColumnOptions<T> options = new ColumnOptions<T>(columnContentFn, columnOptions.size());
		StyledCellLabelProvider labelProvider = new StyledCellLabelProvider(StyledCellLabelProvider.COLORS_ON_SELECTION) {
			@Override
        	public void update(ViewerCell cell) {
        		// TODO reuse and manage SWT resources
        		final KaviListItem<T> kaviListItem = applyCellDefaultStyles(options, cell);
        		resolveCellTextValue(columnContentFn, cell, kaviListItem);
        		if (options.isSearchable())
        			applyCellScoreMatchStyles(cell, kaviListItem);
                super.update(cell);
        	}
		};
		options.setColumn(createTableViewerColumn(tableViewer, labelProvider).getColumn());
		columnOptions.add(options);
		return options;
	}
	
	public void setSortFieldResolver(Function<T, String> sortFieldResolver) {
		this.sortFieldResolver = sortFieldResolver;
	}
	
	@SuppressWarnings("unchecked")
	private KaviListItem<T> applyCellDefaultStyles(final ColumnOptions<T> options, ViewerCell cell) {
		cell.setForeground(fromRegistry(options.getFontColor()));
		cell.setBackground(fromRegistry(options.getBackgroundColor()));
		Font font = createColumnFont(options, cell);
		cell.setFont(font);
		final KaviListItem<T> kaviListItem = (KaviListItem<T>) cell.getElement();
		return kaviListItem;
	}

	private Font createColumnFont(final ColumnOptions<T> options, ViewerCell cell) {
		Font font = options.getFont();
		if (font == null) {
			FontDescriptor fontDescriptor = FontDescriptor.createFrom(cell.getFont()).setStyle(options.getFontStyle());
			font = fontDescriptor.createFont(cell.getControl().getDisplay());
			options.setFont(font);
		}
		return font;
	}
	
	private Color fromRegistry(RGB rgb) {
		String symbolicName = rgb.red + "." + rgb.blue + "." + rgb.green;
		Color color = JFaceResources.getColorRegistry().get(symbolicName);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
		}
		return color;
	}

	private void resolveCellTextValue(BiFunction<T, Integer, String> columnContentFn, ViewerCell cell, final KaviListItem<T> kaviListItem) {
		cell.setText(columnContentFn.apply(kaviListItem.dataItem, table.indexOf((TableItem) cell.getItem())));
	}	
	private void applyCellScoreMatchStyles(ViewerCell cell, final KaviListItem<T> kaviListItem) {
		cell.setStyleRanges(createStyles(kaviListItem.getColumnScore(cell.getColumnIndex()).matches));
	}
	
	public void setSelectionAction(Consumer<T> handleSelectFn) {
		this.handleSelectFn = handleSelectFn;
	}
	
	protected void close() {
		rapidInputPickList.close();
	}

	public void refresh(String filter) {
		if (table == null) return;
		
		final InputCommand inputCommand = InputCommand.parse(filter).get(0);
		tableEntries = new ListRankAndFilter<T>(columnOptions, listContentProvider, rankingStrategy, sortFieldResolver).rankAndFilter(inputCommand);
		alphaColumnConverter = new Base26AlphaBijectiveConverter(tableEntries.size());
		table.removeAll();
		table.setItemCount(tableEntries.size());
		fastSelectItem(inputCommand);
	}

	private void fastSelectItem(final InputCommand inputCommand) {
		// show fast select index if we are typing a fast select expression
		if (inputCommand.fastSelect) {
			int columnWidth = averageCharacterWidth(columnOptions.get(0).getFont()) * alphaColumnConverter.getNumberOfCharacters() + 5;
			columnOptions.get(0).width(columnWidth);
		} else {
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
	
	public void setListRankingStrategy(BiFunction<String, String, Score> rankStringFn) {
		this.rankingStrategy = rankStringFn;
	}

	public void initialize(Composite composite, int defaultOrientation) {
		composite.addDisposeListener((DisposeListener) this::dispose);
		
		tableViewer = new TableViewer(composite, SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL );
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

		table.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				handleSelection();
			}
		});
		
		addColumn((item, rowIndex) -> alphaColumnConverter.toAlpha(rowIndex + 1)).searchable(false).backgroundColor(242, 215, 135).setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
	}
	
	private boolean isMouseEventOverSelection(MouseEvent event) {
		TableItem itemUnderMouse = table.getItem(new Point(event.x, event.y));
		TableItem itemSelection = table.getSelection()[0];
		return itemSelection.equals(itemUnderMouse);
	}
	
    private TableViewerColumn createTableViewerColumn(TableViewer tableViewer, StyledCellLabelProvider labelProvider) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
        viewerColumn.setLabelProvider(labelProvider);
        return viewerColumn;
    }	
    
    public int getTotalColumnWidth() {
    	return Stream.of(table.getColumns()).map(column -> column.getWidth()).reduce((width1, width2) -> width1 + width2).orElse(400);
    }
    
    private StyleRange[] createStyles(List<Integer> matches) {
    	List<StyleRange> styles = new ArrayList<StyleRange>();
    	for (Integer match : matches) {
    		styles.add(new StyleRange(match, 1, null, fromRegistry(new RGB(150,190,255))));
    	}
    	return styles.toArray(new StyleRange[]{});
    }

	@SuppressWarnings("unchecked")
	private void handleSelection() {
		KaviListItem<T> selectedElement = null;
		if (table.getSelectionCount() == 1) {
			selectedElement = (KaviListItem<T>) table.getSelection()[0].getData();
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
		filterText.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
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
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});
		
		filterText.addModifyListener((ModifyListener) event -> refresh(((Text) event.widget).getText()));
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
	
	private void dispose(DisposeEvent e) {
		if (resourceManager != null) {
			resourceManager.dispose();
			resourceManager = null;
		}
	}
}
