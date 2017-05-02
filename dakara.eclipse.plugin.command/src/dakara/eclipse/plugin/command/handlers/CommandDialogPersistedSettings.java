package dakara.eclipse.plugin.command.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;
import org.eclipse.ui.internal.quickaccess.QuickAccessProvider;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;

public class CommandDialogPersistedSettings {
	private final Map<String, QuickAccessProvider> providersMap = new HashMap<>();
	private final String ID = "dakara.eclipse.plugin.command";
	
	private CommanderSettings commanderSettings = new CommanderSettings(new ArrayList<CommandHistoryEntry>());
	static final String DIALOG_SETTINGS_KEY = "DIALOG_SETTINGS";
	
	public CommandDialogPersistedSettings(QuickAccessProvider[] providers) {
		for (int i = 0; i < providers.length; i++) {
			providersMap.put(providers[i].getId(), providers[i]);
		}
	}
	
	public void saveSettings() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(ID);
		Gson gson = new Gson();
		preferences.put(DIALOG_SETTINGS_KEY, gson.toJson(commanderSettings));
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void loadSettings() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(ID);
		Gson gson = new Gson();
		String keyValue = preferences.get(DIALOG_SETTINGS_KEY, null);
		if (keyValue != null) {
			commanderSettings = gson.fromJson(keyValue, CommanderSettings.class );
		}
	}
	
	private QuickAccessElement getCommandElementFromProvider(String commandId, String providerId) {
		QuickAccessProvider quickAccessProvider = (QuickAccessProvider) providersMap.get(providerId);
		return quickAccessProvider.getElementForId(commandId);
	}
	
	public QuickAccessElement getMostRecentCommandByUserInput(String userInput) {
		if (userInput == null) return null;
		for (CommandHistoryEntry entry : commanderSettings.entries) {
			if (userInput.equals(entry.userInput)) {
				System.out.println("perfect match found " + userInput);
				return getCommandElementFromProvider(entry.commandId, entry.providerId);
			}
		}
		return null;
	}
	
	public void addSelectedCommandToHistory(String text, QuickAccessElement element) {
		CommandHistoryEntry newHistoryEntry = new CommandHistoryEntry(element.getId(), element.getProvider().getId(), text);
		commanderSettings.entries.remove(newHistoryEntry);
		commanderSettings.entries.add(0, newHistoryEntry);
	}
	
	public static class CommanderSettings {
		private final List<CommandHistoryEntry> entries;
		public CommanderSettings(List<CommandHistoryEntry> entries) {
			this.entries = entries;
		}
	}
	
	public static class CommandHistoryEntry {
		public final String commandId;
		public final String providerId;
		public final String userInput; 
		public CommandHistoryEntry(String commandId, String providerId, String userInput) {
			this.commandId = commandId;
			this.providerId = providerId;
			this.userInput = userInput;
		}
		
		@Override
		public boolean equals(Object obj) {
			CommandHistoryEntry otherEntry = (CommandHistoryEntry) obj;
			return otherEntry.userInput.equals(userInput);
		}
	}
}
