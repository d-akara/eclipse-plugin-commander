package dakara.eclipse.plugin.kavi.picklist;

import java.util.function.BiFunction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.TableColumn;

public class ColumnOptions<T> {
	private TableColumn column;
	private int columnIndex;
	private boolean searchable = true;
	private int fontStyle = SWT.NONE;
	private RGB fontRGB = new RGB(0,0,0);
	private RGB backgroundRGB = new RGB(255,255,255);
	private Font font;
	private BiFunction<T, Integer, String> columnContentFn;
	
	public ColumnOptions(BiFunction<T, Integer, String> columnContentFn, int columnIndex) {
		this.columnContentFn = columnContentFn;
		this.columnIndex = columnIndex;
	}
	
	public ColumnOptions<T> setColumn(TableColumn column) {
		this.column = column;
		return this;
	}
	
	public int getColumnIndex() {
		return columnIndex;
	}
	
	public ColumnOptions<T> width(int width) {
		column.setWidth(width);
		return this;
	}
	
	public ColumnOptions<T> right() {
		column.setAlignment(SWT.RIGHT);
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
}
