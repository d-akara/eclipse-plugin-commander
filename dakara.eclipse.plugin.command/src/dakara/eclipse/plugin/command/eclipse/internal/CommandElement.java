package dakara.eclipse.plugin.command.eclipse.internal;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.misc.StatusUtil;
import org.eclipse.ui.internal.quickaccess.QuickAccessElement;
import org.eclipse.ui.statushandlers.StatusManager;

@SuppressWarnings("restriction")
public class CommandElement extends QuickAccessElement {
	public static final String separator = " - "; //$NON-NLS-1$
	private ParameterizedCommand parameterizedCommand;
	private final String id;
	private final int hashCode;

	public CommandElement(ParameterizedCommand command, String id, CommandProvider commandProvider) {
		super(commandProvider);
		this.id = id;
		this.parameterizedCommand = command;
		hashCode = id.hashCode() ^ getProvider().getId().hashCode();
	}

	public ParameterizedCommand getParameterizedCommand() {
		return parameterizedCommand;
	}

	@Override
	public void execute() {
		CommandProvider provider = (CommandProvider) getProvider();
		try {
			provider.executeCommand(parameterizedCommand);
		} catch (Exception e) {
			StatusUtil.handleStatus(e, StatusManager.SHOW | StatusManager.LOG);
		}
		return;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (object == null) return false;
		if (!(object instanceof CommandElement)) return false;
		CommandElement element = (CommandElement) object;
		if (!element.id.equals(id)) return false;
		return (element.getProvider().getId().equals(getProvider().getId()));
	}
	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getLabel() {
		final StringBuilder label = new StringBuilder();

		try {
			Command command = parameterizedCommand.getCommand();
			label.append(parameterizedCommand.getName());
			if (command.getDescription() != null && command.getDescription().length() != 0) {
				label.append(separator).append(command.getDescription());
			}
		} catch (NotDefinedException e) {
			label.append(parameterizedCommand.getId());
		}

		return label.toString();
	}

	@Override
	public String getSortLabel() {
		try {
			return parameterizedCommand.getName();
		} catch (NotDefinedException e) {
			return parameterizedCommand.getId();
		}
	}
}
