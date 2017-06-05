package dakara.eclipse.plugin.command.settings;

import org.junit.Test;

import dakara.eclipse.plugin.kavi.picklist.KaviListItem;

public class CommandDialogPersistedSettingsTest {
	@Test
	public void verifyHistory() {
		CommandDialogPersistedSettings settings = new CommandDialogPersistedSettings(10, item -> ((KaviListItem)item).dataItem.toString());
	}
}
