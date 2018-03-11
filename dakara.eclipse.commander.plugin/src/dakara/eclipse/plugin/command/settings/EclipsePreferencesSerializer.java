package dakara.eclipse.plugin.command.settings;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class EclipsePreferencesSerializer<T> {
	private final String prefereneceId;
	private final String preferenceKey;
	private final Class<?> settingsTypeClass;
	private final Gson gson;
	public EclipsePreferencesSerializer(String preferenceId, String preferenceKey, final Class<?> settingsTypeClass) {
		this.prefereneceId = preferenceId;
		this.preferenceKey = preferenceKey;
		this.settingsTypeClass = settingsTypeClass;
		this.gson = new Gson();
	}
	
	public String settingsAsJson(final T settings) {
		return gson.toJson(settings);
	}
	
	public String settingsAsJsonFormatted(final T settings) {
		return new GsonBuilder().setPrettyPrinting().create().toJson(settings);
	}
	
	@SuppressWarnings("unchecked")
	public T jsonAsSettings(String json) {
		return (T) gson.fromJson(json, settingsTypeClass );
	}
	
	public EclipsePreferencesSerializer<T> saveSettings(final T settings) {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(prefereneceId);
		preferences.put(preferenceKey, settingsAsJson(settings));
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	public T loadSettings() {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(prefereneceId);
		String keyValue = preferences.get(preferenceKey, null);
		if (keyValue != null) {
			return jsonAsSettings(keyValue);
		}
		return null;
	}
}
