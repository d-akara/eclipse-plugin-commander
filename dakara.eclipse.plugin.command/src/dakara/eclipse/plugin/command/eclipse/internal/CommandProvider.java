package dakara.eclipse.plugin.command.eclipse.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.commands.ExpressionContext;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;
import org.eclipse.ui.internal.quickaccess.QuickAccessMessages;
import org.eclipse.ui.internal.quickaccess.QuickAccessProvider;

@SuppressWarnings("restriction")
public class CommandProvider extends QuickAccessProvider {
	private final IEvaluationContext evaluationContext;
	private final IHandlerService handlerService;
	private final ICommandService commandService;
	private final EHandlerService ehandlerService;
	private final Map<String, QuickAccessElement> commandById;
	
	public CommandProvider(IEvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
		IEclipseContext context = ((ExpressionContext) evaluationContext).eclipseContext;
		ehandlerService = context.get(EHandlerService.class);
		handlerService  = context.get(IHandlerService.class);
		commandService  = context.get(ICommandService.class);
		commandById = new HashMap<>();
	}

	@Override
	public String getId() {
		return "org.eclipse.ui.commands"; //$NON-NLS-1$
	}

	@Override
	public QuickAccessElement getElementForId(String id) {
		getElements();
		return commandById.get(id);
	}
	
	public QuickAccessElement[] getElements() {
		return Arrays.stream(commandService.getDefinedCommands())
			  .map(command -> new ParameterizedCommand(command, null))
			  .filter(ehandlerService::canExecute)
			  .flatMap(paramCommand -> getCombinations(paramCommand.getCommand()).stream().filter( item -> item != null) )
			  .map(paramCommand -> new CommandElement((ParameterizedCommand) paramCommand, paramCommand.getId(), this))
			  .peek(element -> commandById.put(element.getId(), element))
			  .collect(Collectors.toList())
			  .toArray(new QuickAccessElement[]{});
	}
	
	@SuppressWarnings("unchecked")
	private Collection<ParameterizedCommand> getCombinations(Command command) {
		try {
			return ParameterizedCommand.generateCombinations(command);
		} catch (NotDefinedException e) {}
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return QuickAccessMessages.QuickAccess_Commands;
	}

	public void executeCommand(ParameterizedCommand command) throws ExecutionException, NotDefinedException, NotEnabledException, NotHandledException {
		handlerService.executeCommandInContext(command, null, evaluationContext);
	}
	
	@Override
	protected void doReset() {
		if (evaluationContext instanceof ExpressionContext) {
			((ExpressionContext) evaluationContext).eclipseContext.dispose();
		}
	}
}
