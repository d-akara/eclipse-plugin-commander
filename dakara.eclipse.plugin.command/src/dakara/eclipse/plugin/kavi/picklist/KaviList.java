package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class KaviList<T> {
	private final KaviPickListDialog<T> rapidInputPickList;
	private List<RapidInputTableItem<T>> tableEntries;
	private Function<String, List<T>> listContentProvider;
	private Consumer<T> handleSelectFn;
	private BiFunction<String, String, Score> rankStringFn;
	private List<ColumnOptions<T>> columnOptions = new ArrayList<>();

	private TableViewer tableViewer;
	private Table table;
	private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

	public KaviList(KaviPickListDialog<T> rapidInputPickList) {
		this.rapidInputPickList = rapidInputPickList;
	}

	public void setListContentProvider(Function<String, List<T>> listContentProvider) {
		this.listContentProvider = listContentProvider;
	}
	
	public ColumnOptions<T> addColumn(Function<T, String> columnContentFn) {
		final ColumnOptions<T> options = new ColumnOptions<T>(columnContentFn);
		StyledCellLabelProvider labelProvider = new StyledCellLabelProvider(StyledCellLabelProvider.COLORS_ON_SELECTION) {
        	@SuppressWarnings("unchecked")
			@Override
        	public void update(ViewerCell cell) {
        		Display display = cell.getControl().getDisplay();
        		cell.setForeground(new Color(display, options.getFontColor()));
        		cell.setBackground(new Color(display, options.getBackgroundColor()));
        		FontDescriptor fontDescriptor = FontDescriptor.createFrom(cell.getFont()).setStyle(options.getFontStyle());
        		Font font = fontDescriptor.createFont(cell.getControl().getDisplay());
        		cell.setFont(font);
        		final RapidInputTableItem<T> rapidInputTableItem = (RapidInputTableItem<T>) cell.getElement();
                cell.setText(columnContentFn.apply(rapidInputTableItem.dataItem));
                cell.setStyleRanges(createStyles(rapidInputTableItem.getScore(cell.getColumnIndex()).matches));
                super.update(cell);
        	}
		};
		options.setColumn(createTableViewerColumn(tableViewer, labelProvider).getColumn());
		columnOptions.add(options);
		return options;
	}
	
	public void setSelectionAction(Consumer<T> handleSelectFn) {
		this.handleSelectFn = handleSelectFn;
	}
	
	protected void close() {
		rapidInputPickList.close();
	}

	public void refresh(String filter) {
		if (table != null) {
			tableEntries = listContentProvider.apply(filter).stream().
					       map(item -> new RapidInputTableItem<>(item)).
					       peek(item -> {
					    	   columnOptions.stream().map(options -> options.getColumnContentFn().apply(item.dataItem)).
					    	   		forEach(columnText -> item.addScore(rankStringFn.apply(columnText, filter)));  
					       }).
					       sorted((itemA, itemB) -> Integer.compare(itemB.totalScore(), itemA.totalScore())).
					       filter(item -> item.totalScore() > 0).
						   collect(Collectors.toList());
			table.removeAll();
			table.setItemCount(tableEntries.size());
		}
	}
	
	public void setListRankingStrategy(BiFunction<String, String, Score> rankStringFn) {
		this.rankStringFn = rankStringFn;
	}

	public Table createTable(Composite composite, int defaultOrientation) {
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

		return table;
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
    		styles.add(new StyleRange(match, 1, null, new Color(Display.getCurrent(), 150,190,255)));
    	}
    	return styles.toArray(new StyleRange[]{});
    }

	@SuppressWarnings("unchecked")
	private void handleSelection() {
		RapidInputTableItem<T> selectedElement = null;
		if (table.getSelectionCount() == 1) {
			selectedElement = (RapidInputTableItem<T>) table.getSelection()[0].getData();
		}
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
	
	public static final class RapidInputTableItem<T> {
		private T dataItem;
		private List<Score> scores = new ArrayList<>();
		public RapidInputTableItem(T dataItem) {
			this.dataItem = dataItem;
		}
		public T getDataItem() {
			return dataItem;
		}
		public void addScore(Score score) {
			scores.add(score);
		}
		public Score getScore(int columnIndex) {
			return scores.get(columnIndex);
		}
		
		public int totalScore() {
			return scores.stream().mapToInt(score -> score.rank).sum();
		}
	}
}
