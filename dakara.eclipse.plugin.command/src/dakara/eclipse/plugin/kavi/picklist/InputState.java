package dakara.eclipse.plugin.kavi.picklist;

@SuppressWarnings("rawtypes")
public class InputState {
	public final InputCommand inputCommand;
	public final InternalContentProviderProxy provider;
	public final InternalContentProviderProxy previousProvider;
	
	public InputState(InputCommand inputCommand, InternalContentProviderProxy provider, InternalContentProviderProxy previousProvider) {
		this.inputCommand = inputCommand;
		this.provider = provider;
		this.previousProvider = previousProvider;
	}
}
