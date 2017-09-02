package dakara.eclipse.plugin.command.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import dakara.eclipse.plugin.command.Constants;
import dakara.eclipse.plugin.log.EclipsePluginLogger;

public class PersistedWorkingSet<T> {
	private EclipsePluginLogger logger = new EclipsePluginLogger(Constants.BUNDLE_ID);
	private final int historyLimit;
	private List<HistoryEntry> currentEntries = new ArrayList<>();
	private boolean historyChangedSinceCheck = false;
	
	public final Function<T, HistoryKey> historyItemIdResolver;
	public final Function<HistoryKey, T> historyItemResolver;
	private CommanderSettings commanderSettings = new CommanderSettings(new ArrayList<HistoryEntry>());
	private EclipsePreferencesSerializer<CommanderSettings> eclipsePreferencesSerializer;
	static final String HISTORY_KEY = "HISTORY";
	// TODO separate history and settings store
	// TODO keep long term history of all items
	
	public PersistedWorkingSet(String id, int historyLimit, Function<T, HistoryKey> historyItemIdResolver, Function<HistoryKey, T> historyItemResolver) {
		this.historyLimit = historyLimit;
		this.historyItemIdResolver = historyItemIdResolver;
		this.historyItemResolver = historyItemResolver;
		this.eclipsePreferencesSerializer = new EclipsePreferencesSerializer<>(id, HISTORY_KEY);
	}

	public PersistedWorkingSet<T> save() {
		try {
			eclipsePreferencesSerializer.saveSettings(commanderSettings);
		} catch (Throwable e) {
			logger.error("Unable to save settings", e);
		}
		return this;
	}

	public PersistedWorkingSet<T> load() {
		try {
			commanderSettings = eclipsePreferencesSerializer.loadSettings(CommanderSettings.class);
		} catch (Throwable e) {
			logger.error("Unable to restore settings and history", e);
		}
		if (commanderSettings == null) commanderSettings = new CommanderSettings(new ArrayList<HistoryEntry>());
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
				logger.error("unable to restore history entry " + entry.entryId,  e);
			}
		}
		return currentEntries;
	}
	
	public HistoryEntry getHistoryEntry(T historyItem) {
		HistoryEntry newHistoryEntry = makeEntry(historyItem);
		final int index = commanderSettings.entries.indexOf(newHistoryEntry);
		if (index > -1) return commanderSettings.entries.get(index);
		return null;
	}
	
	public PersistedWorkingSet<T> addToHistory(T historyItem) {
		historyChangedSinceCheck = true;
		HistoryEntry newHistoryEntry = makeEntry(historyItem);
		commanderSettings.entries.remove(newHistoryEntry);
		commanderSettings.entries.add(0, newHistoryEntry);
		if (commanderSettings.entries.size() > historyLimit) {
			commanderSettings.entries.remove(commanderSettings.entries.size() - 1);
		}
		return this;
	}
	
	public PersistedWorkingSet<T> setContentMode(String mode) {
		commanderSettings.contentMode = mode;
		return this;
	}
	
	public String getContentMode() {
		return commanderSettings.contentMode;
	}
	
	public PersistedWorkingSet<T> setHistoryPermanent(T historyItem, boolean permanent) {
		historyChangedSinceCheck = true;
		HistoryEntry entry = makeEntry(historyItem);
		int index = commanderSettings.entries.indexOf(entry);
		if (index == -1) {
			entry.keepForever = true;
			commanderSettings.entries.add(entry);
			return this;
		}
		entry = commanderSettings.entries.get(index);
		entry.keepForever = permanent;
		return this;
	}
	
	public PersistedWorkingSet<T> removeHistory(T historyItem) {
		historyChangedSinceCheck = true;
		commanderSettings.entries.remove(makeEntry(historyItem));
		return this;
	}
	
	public class CommanderSettings {
		private final List<HistoryEntry> entries;
		private String contentMode;
		public CommanderSettings(List<HistoryEntry> entries) {
			this.entries = entries;
		}
	}
	
	private HistoryEntry makeEntry(T item) {
		return new HistoryEntry(historyItemIdResolver.apply(item));
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
			return keys.equals(((HistoryKey)obj).keys);
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
			if (!(obj instanceof PersistedWorkingSet.HistoryEntry)) return false;
			return entryId.equals(((HistoryEntry)obj).entryId);
		}
		
		@Override
		public int hashCode() {
			return entryId.hashCode();
		}
	}
}
