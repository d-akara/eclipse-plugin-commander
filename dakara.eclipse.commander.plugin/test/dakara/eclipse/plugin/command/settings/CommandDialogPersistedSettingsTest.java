package dakara.eclipse.plugin.command.settings;

import org.junit.Assert;
import org.junit.Test;

import dakara.eclipse.plugin.command.Constants;
import dakara.eclipse.plugin.command.settings.PersistedWorkingSet.HistoryKey;

public class CommandDialogPersistedSettingsTest {
	
	
	@Test
	public void verifyHistory() {
		PersistedWorkingSet<TestItem> settings = new PersistedWorkingSet<>(Constants.BUNDLE_ID, false, 10, item -> new HistoryKey(item.field1), historyKey -> new TestItem(historyKey.keys.get(0), null, null) );
		settings.addToHistory(new TestItem("one", null, null));
		Assert.assertEquals("one", settings.getHistory().get(0).getHistoryItem().field1);
	}
	
	private class TestItem {
		public final String field1;
		public final String field2;
		public final String field3;
		
		public TestItem(String field1, String field2, String field3) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
		}
	}	
}
