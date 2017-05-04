package dakara.eclipse.plugin.command.picklist;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.TableColumn;

public class ColumnOptions {
	private TableColumn column;
	private int fontStyle = SWT.NONE;
	private RGB fontRGB = new RGB(0,0,0);
	private RGB backgroundRGB = new RGB(255,255,255);
	public ColumnOptions() {
	}
	
	public void setColumn(TableColumn column) {
		this.column = column;
	}
	
	public ColumnOptions width(int width) {
		column.setWidth(width);
		return this;
	}
	
	public ColumnOptions right() {
		column.setAlignment(SWT.RIGHT);
		return this;
	}
	
	public ColumnOptions italic() {
		fontStyle = SWT.ITALIC;
		return this;
	}
	
	public ColumnOptions fontColor(int red, int green, int blue) {
		fontRGB = new RGB(red, green, blue);
		return this;
	}
	
	public RGB getFontColor() {
		return fontRGB;
	}
	
	public ColumnOptions backgroundColor(int red, int green, int blue) {
		backgroundRGB = new RGB(red, green, blue);
		return this;
	}
	
	public RGB getBackgroundColor() {
		return backgroundRGB;
	}
	
	public int getFontStyle() {
		return fontStyle;
	}
	
}
