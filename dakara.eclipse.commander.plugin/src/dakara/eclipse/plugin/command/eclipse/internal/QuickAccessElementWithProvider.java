package dakara.eclipse.plugin.command.eclipse.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.quickaccess.QuickAccessProvider;
import org.eclipse.ui.quickaccess.QuickAccessElement;

public class QuickAccessElementWithProvider extends QuickAccessElement {
	private QuickAccessElement element;
	private QuickAccessProvider provider;
	
	public QuickAccessElementWithProvider(QuickAccessElement element, QuickAccessProvider provider) {
		this.element = element;
		this.provider = provider;
	}
	
	@Override
	public String getLabel() {
		return element.getLabel();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return element.getImageDescriptor();
	}

	@Override
	public String getId() {
		return element.getId();
	}

	@Override
	public void execute() {
		element.execute();
	}
	
	public QuickAccessProvider getProvider() {
		return provider;
	}

}
