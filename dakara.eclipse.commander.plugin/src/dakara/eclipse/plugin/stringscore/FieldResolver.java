package dakara.eclipse.plugin.stringscore;

import java.util.function.Function;

public class FieldResolver<T> {
	public final String fieldId;
	public final Function<T, String> fieldResolver;
	public FieldResolver(String fieldId, Function<T, String> fieldResolver) {
		this.fieldId = fieldId;
		this.fieldResolver = fieldResolver;
	}
}
