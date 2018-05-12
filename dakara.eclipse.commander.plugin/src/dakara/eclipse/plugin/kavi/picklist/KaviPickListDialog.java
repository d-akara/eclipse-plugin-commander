package dakara.eclipse.plugin.kavi.picklist;

import java.util.List;
import java.util.Set;
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

import dakara.eclipse.plugin.stringscore.RankedItem;
/*
 * TODO - move status info to top of window
 * - add colors to status text.
 * - status text should include more info such as when showing only selected or sorting methods etc.
 * - allow window to be dragged from status handle
 * - maybe extend bottom info area as an optional info area that will display more details about the item under cursor
 *   - could be used to linearly list columns too long to display etc.  would offer a toggle or hot key to show/hide
 */
@SuppressWarnings("restriction")
public class KaviPickListDialog<T> extends PopupDialog {
	private final KaviList<T> kaviList;
	private Text listFilterInputControl;
	private final StatusDisplayInfo displayInfo = new StatusDisplayInfo();
	private boolean autoCloseOnFocusLost = false;

	public KaviPickListDialog() {
		super(ProgressManagerUtil.getDefaultParent(), SWT.RESIZE | SWT.NO_BACKGROUND, true, true, false, true, true, null, "Central Command");
		kaviList = new KaviList<T>(KaviPickListDialog.this);
		kaviList.setListContentChangedAction((list, selections) -> {
			displayInfo.filteredCount = list.size();
			displayInfo.selectedCount = selections.size();
			displayInfo.mode = kaviList.currentContentMode();
			updateInfoDisplay();
		});
		create();
	}

	@Override
	protected Control createTitleControl(Composite parent) {
		listFilterInputControl = new Text(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(listFilterInputControl);
		kaviList.bindInputField(listFilterInputControl);
		kaviList.setFastSelectAction(this::handleFastSelect);
		return listFilterInputControl;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		boolean isWin32 = Util.isWindows();
		GridLayoutFactory.fillDefaults().extendedMargins(isWin32 ? 0 : 3, 3, 2, 2).applyTo(composite);
		kaviList.initialize(composite, getDefaultOrientation());
		return composite;
	}

	@Override
	protected Control getFocusControl() {
		return listFilterInputControl;
	}

	@Override
	public int open() {
		int openResult = super.open();
		kaviList.requestRefresh("");
		return openResult;
	}
	
//	public void hide() {
//		getShell().setVisible(false);
//	}

	public void show() {
		listFilterInputControl.setText("");
		getShell().setVisible(true);
		listFilterInputControl.setFocus();
	}
	
	@Override
	public boolean close() {
		if (autoCloseOnFocusLost) return super.close();
		
		// override so that losing focus does not close dialog
		return false;
	}
	
	public void dismiss() {
		super.close();
	}
	
	@Override
	protected void initializeBounds() {
		// prevent sizing on creation when the column definition have not been added yet.
	}
	
	private void updateInfoDisplay() {
		setInfoText("mode: " + displayInfo.mode + " / items: " + displayInfo.filteredCount + " / selected: " + displayInfo.selectedCount);
	}
	
	public void setBounds(int width, int height) {
		Point size = new Point(width, height);
		Point location = getInitialLocation(size);
		// Add 25 to width to include edge with scroll bar
		// TODO figure out how to set size precisely
		getShell().setBounds(getConstrainedShellBounds(new Rectangle(location.x, location.y, size.x + 25, size.y)));
	}
	
	public <U> InternalContentProviderProxy<U> setListContentProvider(String name, Function<InputState, List<RankedItem<U>>> listContentProvider) {
		return kaviList.setListContentProvider(name, listContentProvider);
	}
	
	public InternalContentProviderProxy<T> setCurrentProvider(String mode) {
		return kaviList.setCurrentProvider(mode);
	}
	
	private void handleFastSelect(Set<RankedItem<T>> selectedItems, InputCommand command) {
		String currentText = listFilterInputControl.getText();
		String newText = currentText.substring(0, currentText.lastIndexOf('/') + 1);
		setFilterInputText(newText);
		displayInfo.selectedCount = selectedItems.size();
		updateInfoDisplay();
	}
	
	public void setFilterInputText(String newText) {
		listFilterInputControl.setText(newText);
		listFilterInputControl.setSelection(newText.length());
	}
	
	public InternalContentProviderProxy<T> togglePreviousProvider() {
		return kaviList.togglePreviousProvider();
	}
	
	public void refresh() {
		kaviList.refresh();
	}
	
	public void setAutoCloseOnFocusLost(boolean autoClose) {
		this.autoCloseOnFocusLost = autoClose;
	}
	
	private class StatusDisplayInfo {
		public String mode;
		public int itemCount;
		public int filteredCount;
		public int selectedCount;
	}
}
