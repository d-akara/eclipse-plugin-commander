package dakara.eclipse.plugin.command.eclipse.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.quickaccess.ActionProvider;
import org.eclipse.ui.internal.quickaccess.EditorProvider;
import org.eclipse.ui.internal.quickaccess.PerspectiveProvider;
import org.eclipse.ui.internal.quickaccess.PreferenceProvider;
import org.eclipse.ui.internal.quickaccess.PropertiesProvider;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;
import org.eclipse.ui.internal.quickaccess.QuickAccessProvider;
import org.eclipse.ui.internal.quickaccess.ViewProvider;
import org.eclipse.ui.internal.quickaccess.WizardProvider;

@SuppressWarnings("restriction")
public class EclipseCommandProvider {
	private List<QuickAccessProvider> providers;
	private List<QuickAccessElement> commandsAvailableWithCurrentContext = new ArrayList<>();
	private Map<String, QuickAccessElement> commandLookupByProviderAndId = new HashMap<>();
	
	public EclipseCommandProvider() {
		initializeWithCurrentContext();
	}

	public EclipseCommandProvider initializeWithCurrentContext() {
		commandsAvailableWithCurrentContext.clear();
		
		WorkbenchWindow workbenchWindow = (WorkbenchWindow) PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		final org.eclipse.e4.ui.model.application.ui.basic.MWindow model = workbenchWindow.getModel();

		providers = new ArrayList<>();
		providers.add(new EditorProvider());
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

	public List<QuickAccessElement> getAllCommands() {
		if (commandsAvailableWithCurrentContext.isEmpty()) {
			for (QuickAccessProvider provider : providers) {
				commandsAvailableWithCurrentContext.addAll(Arrays.asList(provider.getElements()));
			}
			for (QuickAccessElement command: commandsAvailableWithCurrentContext) {
				commandLookupByProviderAndId.put(command.getProvider().getId() + command.getId(), command);
			}
		}
		return commandsAvailableWithCurrentContext;
	}
	
	public QuickAccessElement getCommand(String providerId, String commandId) {
		return commandLookupByProviderAndId.get(providerId + commandId);
	}
}
