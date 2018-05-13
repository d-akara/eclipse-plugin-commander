package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.themes.ColorUtil;

import dakara.eclipse.plugin.kavi.picklist.InternalContentProviderProxy.RowState;
import dakara.eclipse.plugin.stringscore.RankedItem;
import dakara.eclipse.plugin.stringscore.StringScore.Score;
/*
 * TODO - move a colors to a common location.  prior step to allowing user customizations.
 */
public class KaviListColumns<T> {
	private final List<ColumnOptions<T>> columnOptions = new ArrayList<>();
	private final TableViewer tableViewer;
	private final Function<RankedItem<T>, Integer> rowStateResolver;
	public KaviListColumns(TableViewer tableViewer, Function<RankedItem<T>, Integer> rowStateResolver) {
		this.tableViewer = tableViewer;
		this.rowStateResolver = rowStateResolver;
	}
	
	public ColumnOptions<T> addColumn(String columnId, Function<T, String> columnContentFn) {
		return addColumn(columnId, (item, rowIndex) -> columnContentFn.apply(item));
	}
	
	public ColumnOptions<T> addColumn(String columnId, BiFunction<T, Integer, String> columnContentFn) {
		final ColumnOptions<T> options = new ColumnOptions<T>(this, columnId, columnContentFn, columnOptions.size());
		StyledCellLabelProvider labelProvider = new StyledCellLabelProvider(StyledCellLabelProvider.COLORS_ON_SELECTION) {
			@Override
	        	public void update(ViewerCell cell) {
	        		// TODO reuse and manage SWT resources
	        		final RankedItem<T> rankedItem = applyCellDefaultStyles(options, cell);
	        		resolveCellTextValue(columnContentFn, cell, rankedItem);
	        		if (options.isSearchable())
	        			applyCellScoreMatchStyles(cell, rankedItem);
	        		super.update(cell);
	        	}
			@Override
			protected void paint(Event event, Object element) {
				Function<T, Boolean> markerIndicatorProvider = options.getMarkerIndicatorProvider();
				// TODO - make this more generic so we can have different types of markers
				// - possibly allow the provider to draw its own marker
				// - consider passing rankedItem.  We could then draw ranking strength markers
				if (markerIndicatorProvider != null && markerIndicatorProvider.apply(((RankedItem<T>) element).dataItem)) {
					GC gc = event.gc;
					ViewerCell cell = getViewer().getCell(new Point(event.x, event.y));
					Rectangle bounds = cell.getBounds();
					RGB markerColor = new RGB(49, 196, 121);
					
					gc.setForeground(fromRegistry(markerColor));
					gc.setBackground(fromRegistry(markerColor));
					gc.fillRectangle(bounds.x, bounds.y + 2, getAdjustmentForMarkerWidth(), bounds.height - 4);
				}
				super.paint(event, element);
			}
		};
		options.setLabelProvider(labelProvider);
		columnOptions.add(options);
		return options;
	}
	
	public List<ColumnOptions<T>> getColumnOptions() {
		return columnOptions;
	}
	
	public KaviListColumns<T> reset() {
		for (TableColumn column : tableViewer.getTable().getColumns()) {
			column.dispose();
		}
		return this;
	}
	
	public KaviListColumns<T> installColumnsIntoTable() {
		for (ColumnOptions<T> columnOption : columnOptions) {
			columnOption.setColumn(createTableViewerColumn(tableViewer, columnOption.getLabelProvider()).getColumn());
		}
		return this;
	}
	
	public int totalColumnWidth() {
		int width = 0;
		for(ColumnOptions<T> options : columnOptions) {
			width += options.width();
		}
		return width;
	}
	
	public int totalFixedColumnWidth() {
		int width = 0;
		for(ColumnOptions<T> options : columnOptions) {
			if (options.widthPercent() == 0)
				width += options.width();
		}
		return width;
	}
	
    private TableViewerColumn createTableViewerColumn(TableViewer tableViewer, StyledCellLabelProvider labelProvider) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE | SWT.RESIZE);
        viewerColumn.setLabelProvider(labelProvider);
        return viewerColumn;
    }	
    
	
	@SuppressWarnings("unchecked")
	private RankedItem<T> applyCellDefaultStyles(final ColumnOptions<T> options, ViewerCell cell) {
		final RankedItem<T> rankedItem = (RankedItem<T>) cell.getElement();
		cell.setForeground(fromRegistry(options.getFontColor()));
		int rowState = rowStateResolver.apply(rankedItem);
		
	    if ((rowState & RowState.SELECTED.value) != 0 && options.isEnableBackgroundSelection()) {
			cell.setBackground(fromRegistry(new RGB(225,226,206)));
		} else {
			cell.setBackground(fromRegistry(options.getBackgroundColor()));
		}
	    if ((rowState & RowState.CURSOR.value) != 0 && options.isEnableBackgroundSelection()) {
			cell.setBackground(fromRegistry(ColorUtil.blend(cell.getBackground().getRGB(), new RGB(200,200,200))));
	    }
		Font font = createColumnFont(options, cell);
		cell.setFont(font);
		return rankedItem;
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
			JFaceResources.getColorRegistry().put(symbolicName, color.getRGB());
		}
		return color;
	}
	
	private void resolveCellTextValue(BiFunction<T, Integer, String> columnContentFn, ViewerCell cell, final RankedItem<T> rankedItem) {
		cell.setText(columnContentFn.apply(rankedItem.dataItem, tableViewer.getTable().indexOf((TableItem) cell.getItem())));
	}	
	private void applyCellScoreMatchStyles(ViewerCell cell, final RankedItem<T> rankedItem) {
		Score score = rankedItem.getColumnScore(getColumnIdFromColumnIndex(cell.getColumnIndex()));
		if (score != null) {
			// TODO - investigate performance options
			// this is currently the bottle neck in UI performance.  Creating and setting styles.
			cell.setStyleRanges(createStyles(score.matches));
		}
	}
	
	private String getColumnIdFromColumnIndex(int columnIndex) {
		// TODO consider: likely there will never be lots of columns.  Looping is probably optimal here vs a lookup table.
		for (ColumnOptions<T> options : columnOptions) {
			if (options.columnIndex == columnIndex) return options.columnId;
		}
		throw new IllegalStateException("No matching column index");
	}
	
    private StyleRange[] createStyles(List<Integer> matches) {
	    	List<StyleRange> styles = new ArrayList<StyleRange>();
	    	for (Integer match : matches) {
	    		styles.add(new StyleRange(match, 1, null, fromRegistry(new RGB(150,190,255))));
	    	}
	    	return styles.toArray(new StyleRange[]{});
    }
    
    private int getAdjustmentForMarkerWidth() 	{return SWT.getPlatform().equals("win32") ? 3  : 2;}
}
