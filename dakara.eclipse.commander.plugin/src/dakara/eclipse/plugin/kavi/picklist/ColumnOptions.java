package dakara.eclipse.plugin.kavi.picklist;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.TableColumn;

public class ColumnOptions<T> {
	private TableColumn column;
	final public int columnIndex;
	final public String columnId;
	private boolean searchable = true;
	private int fontStyle = SWT.NONE;
	private RGB fontRGB = new RGB(0,0,0);
	private RGB backgroundRGB = new RGB(255,255,255);
	private Font font;
	private BiFunction<T, Integer, String> columnContentFn;
	private Function<T, Boolean> markerIndicatorProvider;
	private KaviListColumns<T> kaviListColumns;
	private StyledCellLabelProvider labelProvider;
	private int columnWidth = 100;
	private int columnAlignment = SWT.LEFT;
	private int columnWidthPercent = 0;
	private boolean enableBackgroundSelection = true;
	
	public ColumnOptions(KaviListColumns<T> kaviListColumns, String columnId, BiFunction<T, Integer, String> columnContentFn, int columnIndex) {
		this.columnContentFn = columnContentFn;
		this.columnIndex = columnIndex;
		this.columnId = columnId;
		this.kaviListColumns = kaviListColumns;
	}
	
	public ColumnOptions<T> setColumn(TableColumn column) {
		this.column = column;
		column.setWidth(columnWidth);
		column.setAlignment(columnAlignment);
		return this;
	}
	
	public int getColumnIndex() {
		return columnIndex;
	}
	
	public ColumnOptions<T> width(int width) {
		this.columnWidth = width;
		if (column != null) column.setWidth(width);
		return this;
	}
	
	public ColumnOptions<T> widthPercent(int width) {
		this.columnWidthPercent = width;
		return this;
	}
	
	public int widthPercent() {
		return this.columnWidthPercent;
	}
	
	public int width() {
		return column.getWidth();
	}
	
	public ColumnOptions<T> changeWidth(int change) {
		column.setWidth(column.getWidth() + change);
		return this;
	}
	
	public ColumnOptions<T> right() {
		this.columnAlignment = SWT.RIGHT;
		if (column != null) column.setAlignment(SWT.RIGHT);
		return this;
	}
	
	public ColumnOptions<T> italic() {
		fontStyle = SWT.ITALIC;
		return this;
	}
	
	public ColumnOptions<T> fontColor(int red, int green, int blue) {
		fontRGB = new RGB(red, green, blue);
		return this;
	}
	
	public ColumnOptions<T> searchable(boolean canSearch) {
		this.searchable = canSearch;
		return this;
	}
	
	public boolean isSearchable() {
		return searchable;
	}
	
	public RGB getFontColor() {
		return fontRGB;
	}
	
	public ColumnOptions<T> backgroundColor(int red, int green, int blue) {
		backgroundRGB = new RGB(red, green, blue);
		return this;
	}
	
	public RGB getBackgroundColor() {
		return backgroundRGB;
	}
	
	public int getFontStyle() {
		return fontStyle;
	}
	
	public BiFunction<T, Integer, String> getColumnContentFn() {
		return columnContentFn;
	}
	
	public ColumnOptions<T> setFont(Font font) {
		this.font = font;
		return this;
	}
	
	public Font getFont() {
		return this.font;
	}
	
	public void setLabelProvider(StyledCellLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}
	
	public StyledCellLabelProvider getLabelProvider() {
		return labelProvider;
	}
	
	public ColumnOptions<T> addColumn(String columnId, Function<T, String> columnContentFn) {
		return kaviListColumns.addColumn(columnId, columnContentFn);
	}
	
	public TableColumn getColumn() {
		return column;
	}
	
	public boolean isEnableBackgroundSelection() {
		return enableBackgroundSelection;
	}
	
	public void setEnableBackgroundSelection(boolean enableBackgroundSelection) {
		this.enableBackgroundSelection = enableBackgroundSelection;
	}
	
	public Function<T, Boolean> getMarkerIndicatorProvider() {
		return markerIndicatorProvider;
	}
	
	public ColumnOptions<T> setMarkerIndicatorProvider(Function<T, Boolean> markerIndicatorProvider) {
		this.markerIndicatorProvider = markerIndicatorProvider;
		return this;
	}
}
