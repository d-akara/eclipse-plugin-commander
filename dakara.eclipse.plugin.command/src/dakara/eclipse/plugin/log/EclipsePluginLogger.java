package dakara.eclipse.plugin.log;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class EclipsePluginLogger {
	private final String bundleId;
	private final ILog logger;
	public EclipsePluginLogger(String bundleId) {
		this.bundleId = bundleId;
		final Bundle bundle = Platform.getBundle(bundleId);
		logger = Platform.getLog(bundle);
	}
	
	public void info(String message) {
		logger.log(new Status(IStatus.INFO, bundleId, message));
	}
	
	public void info(String message, Throwable error) {
		logger.log(new Status(IStatus.ERROR, bundleId, message, error));
	}
	
}
