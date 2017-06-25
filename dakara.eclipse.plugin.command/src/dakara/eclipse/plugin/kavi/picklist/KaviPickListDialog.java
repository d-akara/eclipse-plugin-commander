package dakara.eclipse.plugin.kavi.picklist;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.progress.ProgressManagerUtil;

import dakara.eclipse.plugin.command.settings.CommandDialogPersistedSettings;
import dakara.eclipse.plugin.stringscore.RankedItem;
/*
 * TODO - add page numbers to bottom
 * add item count
 * selected count
 */
@SuppressWarnings("restriction")
public class KaviPickListDialog<T> extends PopupDialog {
	private CommandDialogPersistedSettings persistedSettings;
	private KaviList<T> kavaList;
	private Text listFilterInputControl;

	public KaviPickListDialog() {
		super(ProgressManagerUtil.getDefaultParent(), SWT.RESIZE, true, true, false, true, true, null, "Central Command");
		// persistedSettings = new CommandDialogPersistedSettings();
		kavaList = new KaviList<T>(KaviPickListDialog.this);
		kavaList.setListContentChangedAction(list -> setInfoText("items: " +list.size()));
		create();
		// persistedSettings.loadSettings();
	}

	@Override
	protected Control createTitleControl(Composite parent) {
		listFilterInputControl = new Text(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(listFilterInputControl);
		kavaList.bindInputField(listFilterInputControl);
		return listFilterInputControl;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		boolean isWin32 = Util.isWindows();
		GridLayoutFactory.fillDefaults().extendedMargins(isWin32 ? 0 : 3, 3, 2, 2).applyTo(composite);
		kavaList.initialize(composite, getDefaultOrientation());
		return composite;
	}

	@Override
	protected Control getFocusControl() {
		return listFilterInputControl;
	}

	@Override
	public int open() {
		int openResult = super.open();
		kavaList.refresh("");
		return openResult;
	}

	@Override
	protected Point getDefaultSize() {
		return new Point(kavaList.getTotalColumnWidth(), 400);
	}
	
	@Override
	protected void adjustBounds() {
		Point size = getDefaultSize();
		Point location = getInitialLocation(size);
		// Add 25 to width to include edge with scroll bar
		// TODO figure out how to set size precisely
		getShell().setBounds(getConstrainedShellBounds(new Rectangle(location.x, location.y, size.x + 25, size.y)));
	}

	public void setResolvedAction(Consumer<T> handleSelectFn) {
		kavaList.setSelectionAction(handleSelectFn);
	}

	public ColumnOptions<T> addColumn(String columnId, Function<T, String> columnContentFn) {
		return kavaList.addColumn(columnId, columnContentFn);
	}
	
	public void setListContentProvider(Function<InputCommand, List<RankedItem<T>>> listContentProvider) {
		kavaList.setListContentProvider(listContentProvider);
	}

	public void setContentModes(String ... modes) {
		kavaList.setContentModes(modes);
		
	}
}
