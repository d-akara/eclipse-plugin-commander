package dakara.eclipse.plugin.baseconverter;

import java.util.stream.IntStream;

public class Base26AlphaBijectiveConverter {
	private static final int ASCII_ALPHA_OFFSET = 97;
	private final int maxValue;
	
	public Base26AlphaBijectiveConverter() {
		maxValue = 0;
	}
	
	public Base26AlphaBijectiveConverter(int maxValue) {
		this.maxValue = maxValue;
	}
	public String toAlpha(int num) {
		num = offSetInputBasedOnMaxValue(num);
		
		StringBuffer result = new StringBuffer();
		while (num > 0) {
			num--;
			int remainder = num % 26;
			char digit = (char) (remainder + ASCII_ALPHA_OFFSET);
			result.insert(0, digit);
			num = (num - remainder) / 26;
		}

		return result.toString();
	}
	
	private int offSetInputBasedOnMaxValue(int number) {
		if (maxValue == 0) return number;
		return number += getStartingValue(maxValue) - 1;
	}
	
	public int toNumeric(String alpha) {
		int result = 0;
		int stringIndex = alpha.length();
		int iteration = 0;
		while (iteration++ < stringIndex) {
			char letter = alpha.charAt(stringIndex - iteration);
			int letterValue = letter - ASCII_ALPHA_OFFSET + 1;
			result += letterValue * (Math.pow(26, iteration - 1));
		}

		return offSetResultBasedOnMaxValue(result);
	}
	
	private int offSetResultBasedOnMaxValue(int number) {
		if (maxValue == 0) return number;
		return number -= getStartingValue(maxValue) - 1;
	}
	
	public int getNumberOfCharacters() {
		return calcuteNumberOfCharactersRequired(maxValue);
	}
	
	/*
	 * The intention here is to determine the minimum count of Alpha characters required to represent the maximum value.
	 * This is for the case where we have a list of items and want the list to be enumerated with the same number of alpha characters like:
	 * 'aaa' 
	 * 'aab'
	 * 'aac'
	 * So there is not a mix of 'a' 'aa' 'aaa' etc.
	 * 
	 *  From the minimum number of alpha characters required, we then compute the numeric starting value
	 *
	 */
	private int getStartingValue(int maxValue) {
		int numCharactersRequired = calcuteNumberOfCharactersRequired(maxValue);
		
		if ( numCharactersRequired == 1) return 1;
		
		return (int) getMaxValueCanBeRepresented(numCharactersRequired) + 1;
	}

	private int calcuteNumberOfCharactersRequired(int maxValue) {
		int range = 0;
		int numCharactersRequired = 0;
		while (range < maxValue) {
			range += (int) Math.pow(26, numCharactersRequired + 1); // number of values represented by this number of characters
			if (numCharactersRequired > 0)
				range -= (int) Math.pow(26, numCharactersRequired);  // subtract number of values represented by previous number of characters
			numCharactersRequired++;
		}
		return numCharactersRequired;
	}
	
	private int getMaxValueCanBeRepresented(int numCharacters) {
		return IntStream.range(1, numCharacters)
			.map(value -> (int)Math.pow(26, value))
			.sum();
	}
}
