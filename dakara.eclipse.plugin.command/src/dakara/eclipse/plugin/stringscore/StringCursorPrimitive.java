package dakara.eclipse.plugin.stringscore;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class StringCursorPrimitive {
	int F_UPPERCASE = 0x1 ;
	int F_WORD_PARTIAL_START 	= 0x1 << 1;
	int F_WORD_PARTIAL_END 	 	= 0x1 << 2;
	int F_ALPHA	   			 	= 0x1 << 3;
	int F_WORDSTART 				= 0x1 << 4;
	int F_WORDEND   				= 0x1 << 5;
	
	char[] text;
	int[] properties;
	String textAsString = null;
	
	public StringCursorPrimitive(String text) {
		analyzeAndTransform(text);
	}
	public StringCursorPrimitive(StringCursorPrimitive primitive) {
		this.text = primitive.text;
		this.properties = primitive.properties;
	}
	public StringCursorPrimitive(char[] text, int[] properties) {
		this.text = text;
		this.properties = properties;
	}
	
	public static StringCursorPrimitive makePrimitiveWithMask(StringCursorPrimitive primitive, IntArrayList masks) {
		char[] text = Arrays.copyOf(primitive.text, primitive.text.length);
		int[] properties = Arrays.copyOf(primitive.properties, primitive.properties.length);
		for (int mask : masks) {
			text[mask] = ' ';
			properties[mask] = 0;
		}
		return new StringCursorPrimitive(text, properties);
	}	
	
	public char charAt(int index) {return text[index];}
	public int length()	{return text.length;}
	
	public int indexOf(final String string) {
		return indexOf(string.toCharArray(), 0);
	}
	public int indexOf(final String string, final int startingOffset) {
		return indexOf(string.toCharArray(), startingOffset);
	}
	
	public int indexOf(final char[] needle, final int startingOffset) {
		final int max = text.length - needle.length+1;
		for(int offsetForCompare = startingOffset; offsetForCompare < max; ++offsetForCompare) {
			boolean found = true;
			for(int indexToCompare = 0; indexToCompare < needle.length; ++indexToCompare) {
				if (text[offsetForCompare+indexToCompare] != needle[indexToCompare]) {
					found = false;
					break;
				}
			}
			if (found) return offsetForCompare;
		}
		return -1;  
	}  

	public int indexOf(final char needle, final int startingOffset) {
        for(int indexToCompare = 0; indexToCompare < text.length; ++indexToCompare) {
           if (text[indexToCompare] == needle) {
               return indexToCompare;
           }
        }
	   return -1;  
	}  
	
	public String asString() {
		if (textAsString != null) return textAsString;
		textAsString = new String(text);
		return textAsString;
	}
	
	public String substring(int start, int end) {
		char[] subArray = new char[end - start];
		System.arraycopy(text, start, subArray, 0, end-start);
		return new String(subArray);
	}
	
	private void analyzeAndTransform(String inputText) {
		char[] originalChars = inputText.toCharArray();
		text = new char[originalChars.length];
		properties = new int[originalChars.length];
		int index = 0;
		for(char originalChar : originalChars) {
			text[index] = (char)Character.toLowerCase((int)originalChar);
			
			// is character uppercase
			if (text[index] != originalChar) properties[index] |= F_UPPERCASE;

			// is character alpha
			if (Character.isAlphabetic(text[index])) properties[index] |= F_ALPHA;
			
			// is word start
			if ((properties[index]     & F_ALPHA) == F_ALPHA &&  				// current char is alpha
				(index == 0 || (properties[index - 1] & F_ALPHA) == 0)) {		// previous char is not alpha or there is no previous char
				properties[index] |= F_WORDSTART;
			}
			
			// is word end
			if ((properties[index]     & F_ALPHA) == F_ALPHA && index == text.length - 1)                         // current char is alpha and is last char
				properties[index] |= F_WORDEND;																	
			else if (index > 0 && (properties[index] & F_ALPHA) == 0 && (properties[index - 1] & F_ALPHA) == F_ALPHA) {		// current char is non alpha and previous is alpha
				properties[index - 1] |= F_WORDEND;
			}
			
			// is transition camel case
			if (index > 0 &&
				(properties[index]     & F_UPPERCASE) == F_UPPERCASE &&  // current char is upper case
				(properties[index - 1] & F_UPPERCASE) == 0 &&		    // previous char is lower case
				(properties[index - 1] & F_ALPHA) == F_ALPHA) {		    		// previous char is alpha
					properties[index] |= F_WORD_PARTIAL_START;
					properties[index-1] |= F_WORD_PARTIAL_END;
					
			}
			index++;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(makeRuler(F_UPPERCASE, 'U')).append('\n');
		builder.append(text).append('\n');
		builder.append(makeRuler(F_WORD_PARTIAL_START, 'C').append('\n'));
		builder.append(makeRuler(F_ALPHA, 'A').append('\n'));
		builder.append(makeRuler(F_WORDSTART, 'W').append('\n'));
		builder.append(makeRuler(F_WORDEND, 'w').append('\n'));
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
