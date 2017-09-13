package dakara.eclipse.plugin.command.settings;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;

public class EclipsePreferencesSerializer<T> {
	private final String prefereneceId;
	private final String preferenceKey;
	
	public EclipsePreferencesSerializer(String preferenceId, String preferenceKey) {
		this.prefereneceId = preferenceId;
		this.preferenceKey = preferenceKey;
	}
	
	public EclipsePreferencesSerializer<T> saveSettings(final T settings) {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(prefereneceId);
		Gson gson = new Gson();
		preferences.put(preferenceKey, gson.toJson(settings));
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public T loadSettings(final Class<?> settingsTypeClass) {
		IEclipsePreferences preferences = ConfigurationScope.INSTANCE.getNode(prefereneceId);
		Gson gson = new Gson();
		String keyValue = preferences.get(preferenceKey, null);
		if (keyValue != null) {
			return (T) gson.fromJson(keyValue, settingsTypeClass );
		}
		return null;
	}
}
