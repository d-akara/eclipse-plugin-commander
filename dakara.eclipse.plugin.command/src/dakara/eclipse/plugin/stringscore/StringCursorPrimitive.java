package dakara.eclipse.plugin.stringscore;

import java.util.Arrays;

public class StringCursorPrimitive {
	int F_UPPERCASE = 0x1;
	int F_CAMELCASE = 0x2;
	
	char[] text;
	int[] properties;
	
	public StringCursorPrimitive(String text) {
		analyzeAndTransform(text);
	}
	public StringCursorPrimitive(StringCursorPrimitive primitive) {
		this.text = primitive.text;
		this.properties = primitive.properties;
	}
	
	private void analyzeAndTransform(String inputText) {
		char[] originalChars = inputText.toCharArray();
		text = new char[originalChars.length];
		properties = new int[originalChars.length];
		int index = 0;
		for(char originalChar : originalChars) {
			text[index] = Character.toLowerCase(originalChar);
			
			// is character uppercase
			if (text[index] != originalChar) properties[index] |= F_UPPERCASE;
			
			// is transition camel case
			if (index > 0 &&
					(properties[index]     & F_UPPERCASE) == F_UPPERCASE &&  // current char is upper case
					(properties[index - 1] & F_UPPERCASE) == 0) {		    // previous char is lower case
				properties[index] |= F_CAMELCASE;
			}
			index++;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(makeRuler(F_UPPERCASE, 'U')).append('\n');
		builder.append(text).append('\n');
		builder.append(makeRuler(F_CAMELCASE, 'C').append('\n'));
		return builder.toString();
	}
	
	private StringBuilder makeRuler(int propertyFlag, char propertyMarkerChar) {
		StringBuilder builder = makeFilledBuilder(text.length, ' ');
		int index = 0;
		while (index < text.length) {
			if ((properties[index] & propertyFlag) == propertyFlag)
				builder.setCharAt(index, propertyMarkerChar);
			index++;
		}
		return builder;
	}
	
	public static StringBuilder makeFilledBuilder(int length,  char fillChar) {
		StringBuilder builder = new StringBuilder(length);
		char[] fillArray = new char[length];
		Arrays.fill(fillArray, fillChar);
		builder.append(fillArray);
		return builder;
	}

}
