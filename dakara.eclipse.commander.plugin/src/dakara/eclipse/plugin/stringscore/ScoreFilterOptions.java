package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreFilterOptions {
	public static final ScoreFilterOptions EMPTY = new ScoreFilterOptions("");
	public String rawInputText;
	public StringCursorPrimitive filterTextCursorPrimitive;
	public boolean scoreAsAcronym = false;
	public boolean scoreAsLiteral = false;
	public boolean inverseMatch = false;
	@SuppressWarnings("unchecked")
	public List<String> inverseFilters = Collections.EMPTY_LIST;
	public ScoreFilterOptions(String rawInputText) {
		parseInput(rawInputText);
	}
	
	private void parseInput(String rawInputText) {
		scoreAsAcronym = false;
		scoreAsLiteral = false;
		this.rawInputText = rawInputText;
		
		if (rawInputText == null || rawInputText.length() == 0) {
			filterTextCursorPrimitive = StringCursorPrimitive.EMPTY;
			return;
		}

		String trimmedInput = rawInputText.trim();
		
		if (rawInputText.charAt(0) == ' ') scoreAsAcronym = true;
		trimmedInput = parseInverseFilters(trimmedInput);  // will remove inverse tokens
		if (inverseMatch == true) {
			if (trimmedInput.length() > 0 && trimmedInput.charAt(trimmedInput.length() - 1) == ' ') scoreAsLiteral = true;
			trimmedInput = trimmedInput.trim();
		} else {
			if (rawInputText.charAt(rawInputText.length() - 1) == ' ') scoreAsLiteral = true;
		}
		
	    filterTextCursorPrimitive  = new StringCursorPrimitive(trimmedInput);
	}
	
	private String parseInverseFilters(String input) {
		if (input.length() == 0) return input;
		String[] filters = input.split("!");
		if (filters.length == 1) return filters[0];  // no split token or split token at end of string
		
		inverseMatch = true;
		inverseFilters = new ArrayList<>();
		for(String inverseFilter : filters) {
			inverseFilters.add(inverseFilter.toLowerCase());
		}
		
		// The first item in the split will be the positive filter text.  All other items are negative filters.
		return inverseFilters.size() > 0 ? inverseFilters.remove(0) : "";
	}
}
