package dakara.eclipse.plugin.command.eclipse.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;

public class LauncherElement extends QuickAccessElement {
	private final String name;
	private final String typeName;
	private final String id;
	
	public LauncherElement(LaunchProvider provider, String id, String name, String typeName) {
		super(provider);
		this.id = id;
		this.name = name;
		this.typeName = typeName;
	}
	
	@Override
	public String getLabel() {
		return "Launch - " + typeName + ": " + name;
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
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LauncherElement)) return false;
		return id.equals(((LauncherElement)obj).id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
}
