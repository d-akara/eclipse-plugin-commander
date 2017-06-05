package dakara.eclipse.plugin.command.settings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;

public class CommandDialogPersistedSettings {
	private final String ID = "dakara.eclipse.plugin.command";
	private final int historyLimit;
	private final Function<Object, String> historyItemIdResolver;
	private CommanderSettings commanderSettings = new CommanderSettings(new ArrayList<HistoryEntry>());
	static final String DIALOG_SETTINGS_KEY = "DIALOG_SETTINGS";
	// TODO separate history and settings store
	// TODO keep a recent history of last 100
	// TODO keep long term history of all items
	// show 1 most recent always at top
	// show next 10-20 most frequent
	// remaining list of long term history
	public CommandDialogPersistedSettings(int historyLimit, Function<Object, String> historyItemIdResolver) {
		this.historyLimit = historyLimit;
		this.historyItemIdResolver = historyItemIdResolver;
	}

	public CommandDialogPersistedSettings saveSettings() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(ID);
		Gson gson = new Gson();
		preferences.put(DIALOG_SETTINGS_KEY, gson.toJson(commanderSettings));
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}

	public CommandDialogPersistedSettings loadSettings() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(ID);
		Gson gson = new Gson();
		String keyValue = preferences.get(DIALOG_SETTINGS_KEY, null);
		if (keyValue != null) {
			commanderSettings = gson.fromJson(keyValue, CommanderSettings.class );
		}
		return this;
	}

	public List<HistoryEntry> getHistory() {
		return commanderSettings.entries;
	}
	
	public CommandDialogPersistedSettings addToHistory(Object historyItem) {
		HistoryEntry newHistoryEntry = new HistoryEntry(historyItemIdResolver.apply(historyItem), System.currentTimeMillis());
		commanderSettings.entries.add(0, newHistoryEntry);
		if (commanderSettings.entries.size() > historyLimit) {
			commanderSettings.entries.remove(commanderSettings.entries.size() - 1);
		}
		return this;
	}
	
	public static class CommanderSettings {
		private final List<HistoryEntry> entries;
		public CommanderSettings(List<HistoryEntry> entries) {
			this.entries = entries;
		}
	}
	
	public static class HistoryEntry {
		public final String commandId;
		public final long time;
		public HistoryEntry(String commandId, long time) {
			this.commandId = commandId;
			this.time = time;
		}
		
		@Override
		public String toString() {
			return new Date(time) + " : " + commandId;
		}
	}
}
