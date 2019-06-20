package dakara.eclipse.plugin.command.eclipse.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.quickaccess.QuickAccessProvider;
import org.eclipse.ui.internal.quickaccess.providers.ActionProvider;
import org.eclipse.ui.internal.quickaccess.providers.PerspectiveProvider;
import org.eclipse.ui.internal.quickaccess.providers.PreferenceProvider;
import org.eclipse.ui.internal.quickaccess.providers.PropertiesProvider;
import org.eclipse.ui.internal.quickaccess.providers.ViewProvider;
import org.eclipse.ui.internal.quickaccess.providers.WizardProvider;
import org.eclipse.ui.quickaccess.QuickAccessElement;

@SuppressWarnings("restriction")
public class EclipseCommandProvider {
	private List<QuickAccessProvider> providers;
	private List<QuickAccessElementWithProvider> commandsAvailableWithCurrentContext = new ArrayList<>();
	private Map<String, QuickAccessElementWithProvider> commandLookupByProviderAndId = new HashMap<>();
	
	public EclipseCommandProvider() {
		initializeWithCurrentContext();
	}

	public EclipseCommandProvider initializeWithCurrentContext() {
		commandsAvailableWithCurrentContext.clear();
		
		WorkbenchWindow workbenchWindow = (WorkbenchWindow) PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		final org.eclipse.e4.ui.model.application.ui.basic.MWindow model = workbenchWindow.getModel();

		providers = new ArrayList<>();
		//providers.add(new EditorProvider());
		providers.add(new ViewProvider(model.getContext().get(MApplication.class), model));
		providers.add(new PerspectiveProvider());
		providers.add(new CommandProvider(new ExpressionContext(model.getContext().getActiveLeaf())));
		providers.add(new ActionProvider());
		providers.add(new PreferenceProvider());
		providers.add(new PropertiesProvider());
		providers.add(new WizardProvider());
		providers.add(new LaunchProvider());
		
		getAllCommands();
		
		return this;
	}

	public List<QuickAccessElementWithProvider> getAllCommands() {
		if (commandsAvailableWithCurrentContext.isEmpty()) {
			for (QuickAccessProvider provider : providers) {
				commandsAvailableWithCurrentContext.addAll(makeElementsWithProvider(provider));
				for (QuickAccessElementWithProvider command: commandsAvailableWithCurrentContext) {
					commandLookupByProviderAndId.put(provider.getId() + command.getId(), command);
				}
			}
		}
		return commandsAvailableWithCurrentContext;
	}
	
	private List<QuickAccessElementWithProvider> makeElementsWithProvider(QuickAccessProvider provider) {
		QuickAccessElement[] elements = provider.getElements();
		List<QuickAccessElementWithProvider> elementsWithProvider = new ArrayList<>();
		for (final QuickAccessElement element : elements) {
			elementsWithProvider.add(new QuickAccessElementWithProvider(element, provider));
		}
		return elementsWithProvider;
	}
	
	public QuickAccessElementWithProvider getCommand(String providerId, String commandId) {
		return commandLookupByProviderAndId.get(providerId + commandId);
	}
}
