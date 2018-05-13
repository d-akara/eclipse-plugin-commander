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
	private CommanderSettings commanderSettings;
	private EclipsePreferencesSerializer<CommanderSettings> eclipsePreferencesSerializer;
	private final String featureId;
	static final String HISTORY_KEY = "HISTORY";
	
	public PersistedWorkingSet(String featureId, boolean workspaceScope, int historyLimit, Function<T, HistoryKey> historyItemIdResolver, Function<HistoryKey, T> historyItemResolver) {
		this.featureId = featureId;
		this.historyLimit = historyLimit;
		this.historyItemIdResolver = historyItemIdResolver;
		this.historyItemResolver = historyItemResolver;
		this.eclipsePreferencesSerializer = new EclipsePreferencesSerializer<>(featureId, workspaceScope, HISTORY_KEY, CommanderSettings.class);
		this.commanderSettings = new CommanderSettings(featureId, new ArrayList<HistoryEntry>());
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
			commanderSettings = migrateSettings(eclipsePreferencesSerializer.loadSettings());
		} catch (Throwable e) {
			logger.error("Unable to restore settings and history", e);
		}
		if (commanderSettings == null) commanderSettings = new CommanderSettings(featureId, new ArrayList<HistoryEntry>());
		return this;
	}
	
	public String settingsAsJson() {
		return eclipsePreferencesSerializer.settingsAsJsonFormatted(commanderSettings);
	}
	
	public void setSettingsFromJson(String jsonSettings) {
		commanderSettings = migrateSettings(eclipsePreferencesSerializer.jsonAsSettings(jsonSettings));
		historyChangedSinceCheck = true;
	}

	private void checkAndClearOnChanged() {
		if (historyChangedSinceCheck) {
			currentEntries = new ArrayList<HistoryEntry>();
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
		final int existingIndex = commanderSettings.entries.indexOf(newHistoryEntry);
		if (existingIndex > -1) newHistoryEntry = commanderSettings.entries.get(existingIndex);  // get existing so we keep other attributes.  We just want to move to first in the list
		commanderSettings.entries.remove(newHistoryEntry);
		commanderSettings.entries.add(0, newHistoryEntry);
		
		// count items that are not favorites and which have a valid item reference
		if (commanderSettings.entries.stream().filter(entry->!entry.keepForever && entry.historyItem != null).count() > historyLimit) {
			HistoryEntry lastRecentEntry = commanderSettings.entries.stream().filter(entry->!entry.keepForever && entry.historyItem != null).reduce((a, b) -> b).orElse(null);
			if(lastRecentEntry != null) 
				commanderSettings.entries.remove(commanderSettings.entries.indexOf(lastRecentEntry));
		}
		return this;
	}
	
	public PersistedWorkingSet<T> setContentMode(String mode) {
		commanderSettings.contentMode = mode;
		return this;
	}
	
	public String getContentMode() {
		String contentMode = "working";
		if (commanderSettings.contentMode != null) 
			contentMode = commanderSettings.contentMode;
		return contentMode;
	}
	
	public PersistedWorkingSet<T> setAutoCloseFocusLost(boolean autoClose) {
		commanderSettings.autoCloseFocusLost = autoClose;
		return this;
	}
	
	public boolean getAutoCloseFocusLost() {
		return commanderSettings.autoCloseFocusLost;
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
	
	public PersistedWorkingSet<T> removeAllNonPermanentHistory() {
		historyChangedSinceCheck = true;
		commanderSettings.entries.removeIf(item -> !item.keepForever);
		return this;
	}
	
	public class CommanderSettings {
		private String featureId;
		private int version = 2;
		private String contentMode;
		private boolean autoCloseFocusLost = false;
		private final List<HistoryEntry> entries;
		public CommanderSettings(String featureId, List<HistoryEntry> entries) {
			this.featureId = featureId;
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
	
	private CommanderSettings migrateSettings(CommanderSettings settings) {
		if (settings == null) return null;
		
		if (settings.version == 0) {
			settings.featureId = this.featureId;
			settings.version = 1;
			settings.contentMode = "working";  // assume those who have already been using Commander are already expect his behavior
		}
		if (settings.version == 1) {
			settings.version = 2;
			settings.autoCloseFocusLost = false; // existing default behavior
		}
		if (!settings.featureId.equals(this.featureId)) {
			throw new RuntimeException("Settings featureId [" + settings.featureId + "] does not match [" + this.featureId + "]");
		}
		return settings;
	}
}
