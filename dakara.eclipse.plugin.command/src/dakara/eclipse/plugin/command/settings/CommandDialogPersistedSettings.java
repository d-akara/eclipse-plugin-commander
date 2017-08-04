package dakara.eclipse.plugin.command.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;

import dakara.eclipse.plugin.log.EclipsePluginLogger;

public class CommandDialogPersistedSettings<T> {
	private EclipsePluginLogger logger = new EclipsePluginLogger("dakara.eclipse.commander.plugin");
	private final String ID = "dakara.eclipse.plugin.command";
	private final int historyLimit;
	private List<HistoryEntry> currentEntries = new ArrayList<>();
	private boolean historyChangedSinceCheck = false;
	
	public final Function<T, HistoryKey> historyItemIdResolver;
	public final Function<HistoryKey, T> historyItemResolver;
	private CommanderSettings commanderSettings = new CommanderSettings(new ArrayList<HistoryEntry>());
	static final String HISTORY_KEY = "HISTORY";
	// TODO separate history and settings store
	// TODO keep long term history of all items
	
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
			try {
				commanderSettings = gson.fromJson(keyValue, CommanderSettings.class );
			} catch (Throwable e) {
				logger.info("Unable to restore settings and history", e);
				commanderSettings = new CommanderSettings(new ArrayList<HistoryEntry>());
			}
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
				entry.historyItem = historyItemResolver.apply(entry.entryId);
				if (entry.historyItem != null)
					currentEntries.add(entry);
			} catch (Exception e) {
				logger.info("unable to restore history entry " + entry.entryId,  e);
			}
		}
		return currentEntries;
	}
	
	public CommandDialogPersistedSettings<T> addToHistory(T historyItem) {
		historyChangedSinceCheck = true;
		HistoryEntry newHistoryEntry = new HistoryEntry(historyItemIdResolver.apply(historyItem));
		commanderSettings.entries.remove(newHistoryEntry);
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
		public final List<String> keys;
		public HistoryKey(String ... keys) {
			this.keys = Arrays.asList(keys);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			return keys.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return keys.hashCode();
		}
	}
	
	public class HistoryEntry {
		public final HistoryKey entryId;
		public boolean keepForever = false;
		private transient T historyItem;
		public HistoryEntry(HistoryKey entryId) {
			this.entryId = entryId;
		}
		
		public T getHistoryItem() {
			return historyItem;
		}
		
		@Override
		public String toString() {
			return entryId.toString();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			return entryId.equals(obj);
		}
		
		@Override
		public int hashCode() {
			return entryId.hashCode();
		}
	}
}
