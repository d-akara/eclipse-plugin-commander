package dakara.eclipse.plugin.stringscore;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * TODO - finish 2nd generation implementation using the StringCursorPrimitive
 * - we should leverage data already stored in the primitive
 * - cleanup methods that might not be used anymore afterwards
 *
 */
public class StringCursor {
	public StringCursorPrimitive text;
	private int indexOfCursor = 0;
	private int currentMarker = 0;
	private IntArrayList markers = new IntArrayList(16);
	public StringCursor(String text) {
		this.text = new StringCursorPrimitive(text);
		//this.text = text;
	}
	
	public StringCursor(StringCursorPrimitive primitive) {
		this.text = primitive;
	}
	
	public StringCursor setMarkers(List<Integer> markers) {
		this.markers = new IntArrayList(markers);
		return this;
	}
	
	public boolean markerPositionTerminal() {
		if (currentMarker == markers.size() || currentMarker == -1) return true;
		return false;
	}
	
	public char currentMarker() {
		if (currentMarker < markers.size())
			return text.charAt(markers.getInt(currentMarker));
		return 0;
	}	
	
	public char peekNextMarker() {
		int nextMarker = currentMarker + 1;
		if (nextMarker < markers.size())
			return text.charAt(markers.getInt(nextMarker));
		return 0;
	}
	
	public char setPreviousMarkCurrent() {
		if (currentMarker > 0) {
			return text.charAt(markers.getInt(currentMarker - 1));
		}
		return 0;
	}
	
	public StringCursor addMark(int indexOfMarkToAdd) {
		if (indexOfMarkToAdd >= text.length()) throw new IllegalArgumentException("Index is greater than text length " +indexOfMarkToAdd);
		
		// insert all marks in order
		int markerNumber = 0;
		int[] markers = this.markers.elements();
		for (int markerIndex = 0; markerIndex < this.markers.size(); markerIndex++) {
			if (indexOfMarkToAdd > markers[markerIndex]) {
				markerNumber++;
			}
		}
		
		this.markers.add(markerNumber, indexOfMarkToAdd);
		//markers.add(indexOfMarkToAdd);
		return this;
	}
	
	public StringCursor clearMarkers() {
		markers.clear();
		return this;
	}
	
	public StringCursor markFillRangeForward(int charsForward) {
		if (charsForward + indexOfCursor > text.length()) throw new IllegalArgumentException("Index is greater than text length. index " + indexOfCursor + " range " + charsForward + " '" + text + "'");
		for (int index = indexOfCursor; index < indexOfCursor + charsForward; index++) {
			addMark(index);
		}
		return this;
	}
	
	public StringCursor markFillAlphaRangeForward(int charsForward) {
		final int originalCursorPosition = indexOfCursor;
		for (int count = 0; count < charsForward; count++) {
			
			addMark(indexOfCursor);
			moveCursorForwardNextAlpha();
		}
		
		indexOfCursor = originalCursorPosition;
		
		return this;
	}
	
	public IntArrayList markers() {
		return markers;
	}
	
	public int countUnMarkedWordsBetweenMarkers(int firstMarker, int lastMarker) {
		if (markers.isEmpty()) return 0;
		int wordCount = 0;
		final int originalCursorPosition = indexOfCursor;
		final int originalMarkerPosition = currentMarker;
		int startIndex = markers.getInt(firstMarker);
		indexOfCursor = startIndex;
		
		setFirstMarkCurrent().setCurrentMarkToEndOfCurrentMarkedRegion();
		while(currentMarker < lastMarker) {
			int nonAlphaRegionCount = countNonAlphabeticRegions(indexOfCurrentMark(), indexOfNextMark());
			if (nonAlphaRegionCount > 1) {
				// We are counting words which are surrounded by whitespace on each side.
				// So there must be 2 non alphabetic regions for this to be true.
				wordCount += nonAlphaRegionCount - 1;
			}
			setNextMarkCurrent().setCurrentMarkToEndOfCurrentMarkedRegion();
		}
		
		indexOfCursor = originalCursorPosition;
		currentMarker = originalMarkerPosition;
		
		return wordCount;
	}
	
	public int countNonAlphabeticRegions(int startIndex, int endIndex) {
		final int originalCursorPosition = indexOfCursor;
		indexOfCursor = startIndex;
		int count = 0;
		while(indexOfCursor < endIndex) {
			if (!Character.isAlphabetic(text.charAt(indexOfCursor))) {
				count++;
				// skip all whitespace
				moveCursorForwardUntil(cursor -> Character.isAlphabetic(cursor.currentChar()));
			} else {
				// skip all chars
				moveCursorForwardUntil(cursor -> !Character.isAlphabetic(cursor.currentChar()));
			}
		}
		indexOfCursor = originalCursorPosition;
		
		return count;
	}
	
	public int countAlphabeticCharsBetween(int startIndex, int endIndex) {
		final int originalCursorPosition = indexOfCursor;
		indexOfCursor = startIndex + 1;
		int count = 0;
		while(indexOfCursor < endIndex) {
			if (Character.isAlphabetic(text.charAt(indexOfCursor))) {
				count++;
			} 
			indexOfCursor++;
		}
		indexOfCursor = originalCursorPosition;
		
		return count;
	}

	public int countPartialWordsBetweenMarkers(int startMarker, int endMarker) {
		if (markers.isEmpty()) return 0;
		int indexOfCursor = markers.getInt(startMarker) + 1;
		final int endIndex = markers.getInt(endMarker);
		return countPartialWordsBetween(indexOfCursor, endIndex);
	}
	
	public int countPartialWordsBetween(int startIndex, int endIndex) {
		int indexOfCursor = startIndex + 1;
		int count = 0;
		while(indexOfCursor < endIndex) {
			if ((text.properties[indexOfCursor] & (text.F_WORD_PARTIAL_START | text.F_WORDSTART)) != 0) {
				count++;
			} 
			indexOfCursor++;
		}
		return count;
	}
	
	public int countWordsBetween(int startIndex, int endIndex) {
		int indexOfCursor = startIndex + 1;
		int count = 0;
		while(indexOfCursor < endIndex) {
			if ((text.properties[indexOfCursor] & (text.F_WORDSTART)) != 0) {
				count++;
			} 
			indexOfCursor++;
		}
		return count;
	}
	
	public StringCursor setCurrentMarkToEndOfCurrentMarkedRegion() {
		if (markerPositionTerminal()) return this;
		
		// move until no more contiguous marks
		while (indexOfNextMark() == indexOfCurrentMark() + 1) {
			setNextMarkCurrent();
		}
		return this;
	}
	
	public boolean currentMarkIsFirstOfMarkedRegion() {
		if (markers.size() == 0) return false;
		if (markerPositionTerminal()) return false;
		if (currentMarker == 0) return true;
		// is previous marker's index next to this marker
		return markers.getInt(currentMarker - 1) != markers.getInt(currentMarker) - 1;
	}
	
	public String wordAtCursor() {
		if (cursorPositionTerminal()) return "";
		int currentIndex = indexOfCursor;
		int indexStart = moveCursorPreviousWordStart().indexOfCursor();
		int indexEnd   = moveCursorForwardWordEnd().indexOfCursor();
		indexOfCursor = currentIndex;
		return text.substring(indexStart, indexEnd + 1);
	}
	
	public String partialWordAtCursor() {
		if (cursorPositionTerminal()) return "";
		int currentIndex = indexOfCursor;
		int indexStart = moveCursorPreviousPartialWordStart().indexOfCursor();
		int indexEnd   = moveCursorForwardPartialWordEnd().indexOfCursor();
		indexOfCursor = currentIndex;
		return text.substring(indexStart, indexEnd + 1);
	}
	
	public String markersAsString() {
		StringBuilder builder = new StringBuilder();
		for(int index : markers) {
			builder.append(text.charAt(index));
		}
		return builder.toString();
	}
	
	public boolean cursorPositionTerminal() {
		if (indexOfCursor == text.length() || indexOfCursor == -1) return true;
		return false;
	}
	
	public boolean cursorAtPartialWordStart() {
		return (text.properties[indexOfCursor] & (text.F_WORD_PARTIAL_START | text.F_WORDSTART)) != 0;
	}
	
	public boolean cursorAtPartialWordEnd() {
		return (text.properties[indexOfCursor] & (text.F_WORD_PARTIAL_END | text.F_WORDEND)) != 0;
	}
	
	public char currentChar() {
		if (indexOfCursor < text.length())
			return text.charAt(indexOfCursor);
		return 0;
	}
	
	public char peekPreviousChar() {
		if (indexOfCursor > 0) {
			return text.charAt(indexOfCursor - 1);
		}
		return 0;
	}
	
	public char peekNextChar() {
		if (indexOfCursor < text.length() - 1) {
			return text.charAt(indexOfCursor + 1);
		}
		return 0;
	}
	
	public StringCursor moveCursorForwardUntil(Function<StringCursor, Boolean> shouldStop) {
		while(!shouldStop.apply(this) && !cursorPositionTerminal()) {
			indexOfCursor++;
		}
		return this;
	}
	
	public StringCursor moveCursorPreviousPartialWordStart() {
		 while(!cursorPositionTerminal()) {
			 if ((text.properties[indexOfCursor] & (text.F_WORD_PARTIAL_START | text.F_WORDSTART)) != 0) break;
			 indexOfCursor--;
		 }
		 return this;
	}
	
	public StringCursor moveCursorForwardPartialWordEnd() {
		while(!cursorPositionTerminal()) {
			if ((text.properties[indexOfCursor] & (text.F_WORD_PARTIAL_END | text.F_WORDEND)) != 0) break;
			indexOfCursor++;
		}
		return this;
	}
	
	public StringCursor moveCursorForwardWordEnd() {
		 while(!cursorPositionTerminal()) {
			 if ((text.properties[indexOfCursor] & (text.F_WORDEND)) != 0) break;
			 indexOfCursor++;
		 }
		 return this;
	}
	
	public StringCursor moveCursorIndexOf(String match) {
		indexOfCursor = text.indexOf(match);
		return this;
	}
	
	public StringCursor moveCursorForwardIndexOf(String match) {
		 indexOfCursor = text.indexOf(match, indexOfCursor);
		 return this;
	}
	
	public StringCursor moveCursorForwardIndexOfAlphaSequenceWrapAround(String match) {
		if (indexOfCursor > 0) {
			moveCursorForwardIndexOfAlphaSequence(match);
			if (cursorPositionTerminal()) {
				// we did not find from current until end.  Try again from beginning
				indexOfCursor = 0;
				moveCursorForwardIndexOfAlphaSequence(match);
			}
		} else {
			moveCursorForwardIndexOfAlphaSequence(match);
		}
		
		return this;
	}
	
	public StringCursor moveCursorForwardIndexOfAlphaSequence(String match) {
		
		 StringCursor matchCursor = new StringCursor(match);
		 int startOfSequenceIndex = -1;
		 while(!matchCursor.cursorPositionTerminal()) {
			 if (matchCursor.indexOfCursor == 0) {
				 moveCursorForwardIndexOf(matchCursor.currentChar());
				 startOfSequenceIndex = indexOfCursor;
				 if (cursorPositionTerminal()) break;				 
			 } else {
				 moveCursorForwardNextAlpha();
				 if (cursorPositionTerminal()) break; 				 
				 if (currentChar() != matchCursor.currentChar()) {
					 // next char was not found. try again from the beginning
					 setCursorPosition(startOfSequenceIndex + 1);
					 matchCursor.setCursorPosition(0);
					 continue;
				 } 				 
			 }
			 matchCursor.moveCursorForward();
		 }
		 
		 // We matched all chars if we are at terminal position.  Reset cursor to initial index
		 if (matchCursor.cursorPositionTerminal()) {
			 indexOfCursor = startOfSequenceIndex;
		 } 
		 
		 return this;
	}
	
	public StringCursor moveCursorForwardIndexOf(char match) {
		 indexOfCursor = text.indexOf(match, indexOfCursor);
		 return this;
	}
	
	public StringCursor moveCursorForward() {
		 indexOfCursor++;
		 return this;
	}
	
	public StringCursor moveCursorForwardNextAlpha() {
		 while(!cursorPositionTerminal()) {
			 indexOfCursor++;
			 if (cursorPositionTerminal() || Character.isAlphabetic(text.charAt(indexOfCursor))) break;
		 }
		 return this;
	}
	
	public StringCursor moveCursorForwardWordStart() {
		 while(!cursorPositionTerminal()) {
			 if ((text.properties[indexOfCursor] & text.F_WORDSTART) == text.F_WORDSTART) break;
			 indexOfCursor++;
		 }
		 return this;
	}
	
	public StringCursor moveCursorPreviousWordStart() {
		 while(!cursorPositionTerminal()) {
			 if ((text.properties[indexOfCursor] & text.F_WORDSTART) == text.F_WORDSTART) break;
			 indexOfCursor--;
		 }
		 return this;
	}
	
	public StringCursor moveCursorForwardPartialWordStart() {
		 while(!cursorPositionTerminal()) {
			 if ((text.properties[indexOfCursor] & (text.F_WORDSTART | text.F_WORD_PARTIAL_START)) != 0) break;
			 indexOfCursor++;
		 }
		 return this;
	}
	
	public StringCursor moveCursorBackward() {
		 indexOfCursor--;
		 return this;
	}	
	
	public StringCursor setCursorPosition(int index) {
		 indexOfCursor = index;
		 return this;
	}	
	
	public StringCursor setNextMarkCurrent() {
		 currentMarker++;
		 return this;
	}
	
	public StringCursor setFirstMarkCurrent() {
		 currentMarker = 0;
		 return this;
	}
	
	public int indexOfFirstMark() {
		return markers.getInt(0);
	}
	
	public int indexOfLastMark() {
		return markers.getInt(markers.size() - 1);
	}
	
	public int lastCharIndex() {
		return text.length() - 1;
	}
	
	public int indexOfCursor() {
		return indexOfCursor;
	}
	
	public int indexOfNextMark() {
		if (!markers.isEmpty() && currentMarker + 1 < markers.size())
			return markers.getInt(currentMarker + 1);
		return 0;
	}
	
	public int indexOfCurrentMark() {
		if (!markers.isEmpty() && !markerPositionTerminal())
			return markers.getInt(currentMarker);
		return -1;
	}
	
	public StringCursor maskRegions(IntArrayList maskIndexes) {
		if (maskIndexes.size() == 0) return this;
		text = StringCursorPrimitive.makePrimitiveWithMask(text, maskIndexes);
		return this;
	}
	
	public StringCursorPrimitive getCursorPrimitive() {
		return text;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(buildMarkerRuler()).append('\n');
		builder.append(text).append('\n');
		builder.append(buildCurrentIndexRuler().append('\n'));
		return builder.toString();
	}
	
	private StringBuilder buildMarkerRuler() {
		StringBuilder builder = makeFilledBuilder(text.length(), ' ');
		for (int markIndex : markers) {
			builder.setCharAt(markIndex, 'M');
		}
		return builder;
	}
	
	private StringBuilder makeFilledBuilder(int length,  char fillChar) {
		StringBuilder builder = new StringBuilder(text.length());
		char[] fillArray = new char[length];
		Arrays.fill(fillArray, fillChar);
		builder.append(fillArray);
		return builder;
	}
	
	private StringBuilder buildCurrentIndexRuler() {
		final int currentIndexMark = indexOfCurrentMark();
		StringBuilder builder = makeFilledBuilder(text.length(), ' ');
		if (currentIndexMark > -1 && indexOfCursor == currentIndexMark) {
			builder.setCharAt(indexOfCursor, '+');
		} else {
			if (indexOfCursor > -1)
				builder.setCharAt(indexOfCursor, '|');
			if (currentIndexMark > -1)
				builder.setCharAt(currentIndexMark, '-');
		}
		return builder;
	}
}
