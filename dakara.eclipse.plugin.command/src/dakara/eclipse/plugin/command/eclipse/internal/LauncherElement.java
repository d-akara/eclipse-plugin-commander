package dakara.eclipse.plugin.command.eclipse.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

public class LauncherElement extends QuickAccessElement {
	private final String name;
	private final String id;
	
	public LauncherElement(LaunchProvider provider, String id, String name) {
		super(provider);
		this.id = id;
		this.name = name;
	}
	
	@Override
	public String getLabel() {
		return name;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void execute() {
		LaunchProvider provider = (LaunchProvider) getProvider();
		provider.execute(this);
	}

}
