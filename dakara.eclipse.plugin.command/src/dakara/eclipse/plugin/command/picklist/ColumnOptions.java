package dakara.eclipse.plugin.command.picklist;

import org.eclipse.swt.widgets.TableColumn;

public class ColumnOptions {
	private TableColumn column;
	public ColumnOptions(TableColumn column) {
		this.column = column;
	}
	
	public ColumnOptions setWidth(int width) {
		column.setWidth(width);
		return this;
	}
	
	public ColumnOptions setAlignment(int alignment) {
		column.setAlignment(alignment);
		return this;
	}
}
