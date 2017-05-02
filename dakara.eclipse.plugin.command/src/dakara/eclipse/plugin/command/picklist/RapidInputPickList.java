package dakara.eclipse.plugin.command.picklist;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.progress.ProgressManagerUtil;

import dakara.eclipse.plugin.command.handlers.CommandDialogPersistedSettings;
import dakara.eclipse.plugin.command.handlers.StringScore.Score;

@SuppressWarnings("restriction")
public class RapidInputPickList<T> extends PopupDialog {
	private CommandDialogPersistedSettings persistedSettings;
	private RapidInputTableWidget<T> rapidInputTableWidget;
	private Text listFilterInputControl;

	public RapidInputPickList() {
		super(ProgressManagerUtil.getDefaultParent(), SWT.RESIZE, true, true, false, true, true, null, "Central Command");
		// persistedSettings = new CommandDialogPersistedSettings();
		rapidInputTableWidget = new RapidInputTableWidget<T>(RapidInputPickList.this);

		// persistedSettings.loadSettings();
		create();
	}

	@Override
	protected Control createTitleControl(Composite parent) {
		listFilterInputControl = new Text(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(listFilterInputControl);
		rapidInputTableWidget.bindInputField(listFilterInputControl);
		return listFilterInputControl;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		boolean isWin32 = Util.isWindows();
		GridLayoutFactory.fillDefaults().extendedMargins(isWin32 ? 0 : 3, 3, 2, 2).applyTo(composite);
		rapidInputTableWidget.createTable(composite, getDefaultOrientation());
		return composite;
	}

	@Override
	protected Control getFocusControl() {
		return listFilterInputControl;
	}

	@Override
	public boolean close() {
		// persistedSettings.saveSettings();
		return super.close();
	}

	@Override
	protected Point getDefaultSize() {
		return new Point(400, 400);
	}

	public void setResolvedAction(Consumer<T> handleSelectFn) {
		rapidInputTableWidget.setSelectionAction(handleSelectFn);
	}

	public void addColumn(Function<T, String> columnContentFn) {
		rapidInputTableWidget.addColumn(columnContentFn);
	}

	public void setListContentProvider(Function<String, List<T>> listContentProvider) {
		rapidInputTableWidget.setListContentProvider(listContentProvider);
	}

	public void setListRankingStrategy(BiFunction<T, String, Score> rankStringFn) {
		rapidInputTableWidget.setListRankingStrategy(rankStringFn);
	}
}
