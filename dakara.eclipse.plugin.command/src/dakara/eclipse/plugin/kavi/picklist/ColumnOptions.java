package dakara.eclipse.plugin.kavi.picklist;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.TableColumn;

public class ColumnOptions<T> {
	private TableColumn column;
	private int fontStyle = SWT.NONE;
	private RGB fontRGB = new RGB(0,0,0);
	private RGB backgroundRGB = new RGB(255,255,255);
	Function<T, String> columnContentFn;
	
	public ColumnOptions(Function<T, String> columnContentFn) {
		this.columnContentFn = columnContentFn;
	}
	
	public void setColumn(TableColumn column) {
		this.column = column;
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
	
	public Function<T, String> getColumnContentFn() {
		return columnContentFn;
	}
	
}
