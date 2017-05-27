package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class StringCursor {
	public final String text;
	private int indexOfCursor = 0;
	private int currentMarker = 0;
	
	private List<Integer> markers = new ArrayList<>();
	public StringCursor(String text) {
		this.text = text;
	}
	
	public StringCursor setMarkers(List<Integer> markers) {
		this.markers = markers;
		return this;
	}
	
	public boolean markerPositionTerminal() {
		if (currentMarker == markers.size() || currentMarker == -1) return true;
		return false;
	}
	
	public char currentMarker() {
		if (currentMarker < markers.size())
			return text.charAt(markers.get(currentMarker));
		return 0;
	}	
	
	public char peekNextMarker() {
		int nextMarker = currentMarker + 1;
		if (nextMarker < markers.size())
			return text.charAt(markers.get(nextMarker));
		return 0;
	}
	
	public char previousMarker() {
		if (currentMarker > 0) {
			return text.charAt(markers.get(currentMarker - 1));
		}
		return 0;
	}
	
	public StringCursor addMarker(int index) {
		if (index >= text.length()) throw new IllegalArgumentException("Index is greater than text length " +index);
		markers.add(index);
		return this;
	}
	
	public StringCursor clearMarkers() {
		markers.clear();
		return this;
	}
	
	public StringCursor markFillRangeForward(int charsForward) {
		if (charsForward + indexOfCursor > text.length()) throw new IllegalArgumentException("Index is greater than text length. index " + indexOfCursor + " range " + charsForward + " '" + text + "'");
		for (int index = indexOfCursor; index < indexOfCursor + charsForward; index++) {
			markers.add(index);
		}
		return this;
	}
	
	public List<Integer> markers() {
		return markers;
	}
	
	
	
	public int wordGapsBetweenMarkedRegions(int firstMarker, int lastMarker) {
		if (markers.isEmpty()) return 0;
		int wordCount = 0;
		final int originalCursorPosition = indexOfCursor;
		final int originalMarkerPosition = currentMarker;
		int startIndex = markers.get(firstMarker);
		indexOfCursor = startIndex;
		
		setCurrentMarkerFirst().advanceMarkerToEndOfCurrentMarkedRegion();
		while(currentMarker < lastMarker) {
			int nonAlphaRegionCount = countNonAlphabeticRegions(indexOfMarker(), indexOfNextMarker());
			if (nonAlphaRegionCount > 1) {
				// We are counting words which are surrounded by whitespace on each side.
				// So there must be 2 non alphabetic regions for this to be true.
				wordCount += nonAlphaRegionCount - 1;
			}
			nextMarker().advanceMarkerToEndOfCurrentMarkedRegion();
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
	
	public StringCursor advanceMarkerToEndOfCurrentMarkedRegion() {
		if (markerPositionTerminal()) return this;
		
		// move until no more contiguous marks
		while (indexOfNextMarker() == indexOfMarker() + 1) {
			nextMarker();
		}
		return this;
	}
	
	public String wordAtCursor() {
		if (cursorPositionTerminal()) return "";
		int currentIndex = indexOfCursor;
		int indexStart = moveCursorPreviousAlphaBoundary().indexOfCursor();
		int indexEnd   = moveCursorNextAlphaBoundary().indexOfCursor();
		indexOfCursor = currentIndex;
		return text.substring(indexStart, indexEnd + 1);
	}
	
	public String markersAsString() {
		StringBuilder builder = new StringBuilder();
		for(Integer index : markers) {
			builder.append(text.charAt(index));
		}
		return builder.toString();
	}
	
	public boolean cursorPositionTerminal() {
		if (indexOfCursor == text.length() || indexOfCursor == -1) return true;
		return false;
	}
	
	public boolean cursorAtWordStart() {
		if (!Character.isAlphabetic(text.charAt(indexOfCursor))) return false;
		return !Character.isAlphabetic(peekPreviousChar());
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
	
	public StringCursor moveCursorForwardUntil(Function<StringCursor, Boolean> shouldContinue) {
		while(!shouldContinue.apply(this) && !cursorPositionTerminal()) {
			indexOfCursor++;
		}
		return this;
	}
	
	public StringCursor moveCursorPreviousAlphaBoundary() {
		while(Character.isAlphabetic(peekPreviousChar())) {
			moveCursorBackward();
		}
		return this;
	}
	
	public StringCursor moveCursorNextAlphaBoundary() {
		while(Character.isAlphabetic(peekNextChar())) {
			moveCursorForward();
		}
		return this;
	}
	
	public StringCursor moveCursorIndexOf(String match) {
		 indexOfCursor = text.indexOf(match);
		 return this;
	}
	
	public StringCursor moveCursorForward() {
		 indexOfCursor++;
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
	
	public StringCursor nextMarker() {
		 currentMarker++;
		 return this;
	}
	
	public StringCursor setCurrentMarkerFirst() {
		 currentMarker = 0;
		 return this;
	}
	
	public int indexOfFirstMarker() {
		return markers.get(0);
	}
	
	public int indexOfLastMarker() {
		return markers.get(markers.size() - 1);
	}
	
	public int indexOfCursor() {
		return indexOfCursor;
	}
	
	public int indexOfNextMarker() {
		if (!markers.isEmpty() && currentMarker + 1 < markers.size())
			return markers.get(currentMarker + 1);
		return 0;
	}
	
	public int indexOfMarker() {
		if (!markers.isEmpty() && !markerPositionTerminal())
			return markers.get(currentMarker);
		return -1;
	}
	
	@Override
	public String toString() {
		return text;
	}
}
