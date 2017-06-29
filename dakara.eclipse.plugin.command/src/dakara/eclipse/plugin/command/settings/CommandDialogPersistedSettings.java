package dakara.eclipse.plugin.command.settings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;

public class CommandDialogPersistedSettings<T> {
	private final String ID = "dakara.eclipse.plugin.command";
	private final int historyLimit;
	private List<HistoryEntry> currentEntries = new ArrayList<>();
	private boolean historyChangedSinceCheck = false;
	
	public final Function<T, HistoryKey> historyItemIdResolver;
	public final Function<HistoryKey, T> historyItemResolver;
	private CommanderSettings commanderSettings = new CommanderSettings(new ArrayList<HistoryEntry>());
	static final String HISTORY_KEY = "HISTORY";
	// TODO separate history and settings store
	// TODO keep a recent history of last 100
	// TODO keep long term history of all items
	// show 1 most recent always at top
	// show next 10-20 most frequent
	// remaining list of long term history
	// show only history when input field empty
	//   add all items when no hits in history
	// alter history item rank so will always be top
	// remove duplicates from main item list and history
	// set mode, only history first or combined.  possibly use tab as toggle
	
	public CommandDialogPersistedSettings(int historyLimit, Function<T, HistoryKey> historyItemIdResolver, Function<HistoryKey, T> historyItemResolver) {
		this.historyLimit = historyLimit;
		this.historyItemIdResolver = historyItemIdResolver;
		this.historyItemResolver = historyItemResolver;
	}

	public CommandDialogPersistedSettings<T> saveSettings() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(ID);
		Gson gson = new Gson();
		preferences.put(HISTORY_KEY, gson.toJson(commanderSettings));
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}

	public CommandDialogPersistedSettings<T> loadSettings() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(ID);
		Gson gson = new Gson();
		String keyValue = preferences.get(HISTORY_KEY, null);
		if (keyValue != null) {
			commanderSettings = gson.fromJson(keyValue, CommanderSettings.class );
		}
		return this;
	}

	private void checkAndClearOnChanged() {
		if (historyChangedSinceCheck) {
			currentEntries.clear();
		}
		historyChangedSinceCheck = false;
	}
	
	public List<HistoryEntry> getHistory() {
		checkAndClearOnChanged();
		
		if (!currentEntries.isEmpty()) return currentEntries;
		
		for (HistoryEntry entry : commanderSettings.entries) {
			try {
				entry.historyItem = historyItemResolver.apply(entry.commandId);
				if (entry.historyItem != null)
					currentEntries.add(entry);
			} catch (Exception e) {
				// TODO - how to report errors to eclipse error log
				System.err.println("unable to restore " + entry.commandId + " due to " + e.getMessage());
				e.printStackTrace();
			}
		}
		return currentEntries;
	}
	
	public CommandDialogPersistedSettings<T> addToHistory(T historyItem) {
		historyChangedSinceCheck = true;
		HistoryEntry newHistoryEntry = new HistoryEntry(historyItemIdResolver.apply(historyItem), System.currentTimeMillis());
		commanderSettings.entries.add(0, newHistoryEntry);
		if (commanderSettings.entries.size() > historyLimit) {
			commanderSettings.entries.remove(commanderSettings.entries.size() - 1);
		}
		return this;
	}
	
	public CommandDialogPersistedSettings<T> setContentMode(String mode) {
		commanderSettings.contentMode = mode;
		return this;
	}
	
	public String getContentMode() {
		return commanderSettings.contentMode;
	}
	
	public class CommanderSettings {
		private final List<HistoryEntry> entries;
		private String contentMode;
		public CommanderSettings(List<HistoryEntry> entries) {
			this.entries = entries;
		}
	}
	
	public static class HistoryKey {
		public final String[] keys;
		public HistoryKey(String ... keys) {
			this.keys = keys;
		}
	}
	
	public class HistoryEntry {
		public final HistoryKey commandId;
		public final long time;
		private transient T historyItem;
		public HistoryEntry(HistoryKey commandId, long time) {
			this.commandId = commandId;
			this.time = time;
		}
		
		public T getHistoryItem() {
			return historyItem;
		}
		
		@Override
		public String toString() {
			return new Date(time) + " : " + commandId;
		}
	}
}
