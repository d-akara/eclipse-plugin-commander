package dakara.eclipse.plugin.command.eclipse.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;
import org.eclipse.ui.internal.quickaccess.QuickAccessProvider;

public class LaunchProvider extends QuickAccessProvider {

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return "Launcher";
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("restriction")
	@Override
	public QuickAccessElement[] getElements() {
		List<QuickAccessElement> elements = new ArrayList<QuickAccessElement>();
		try {
			ILaunchConfiguration[] launchConfigurations = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations();
			for (ILaunchConfiguration configuration : launchConfigurations) {
				elements.add(new LauncherElement(this, configuration.getMemento(), configuration.getName(), configuration.getType().getName()));
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return elements.toArray(new QuickAccessElement[] {});
	}

	public void execute(LauncherElement element) {
		try {
			ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(element.getId());
			configuration.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public QuickAccessElement getElementForId(String id) {
		try {
			ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(id);
			return new LauncherElement(this, configuration.getMemento(), configuration.getName(), configuration.getType().getName());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void doReset() {
		// TODO Auto-generated method stub

	}

}
