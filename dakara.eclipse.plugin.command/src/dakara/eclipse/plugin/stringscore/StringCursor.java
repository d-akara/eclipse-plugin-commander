package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.List;


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
	
	public StringCursor markRangeForward(int charsForward) {
		if (charsForward + indexOfCursor > text.length()) throw new IllegalArgumentException("Index is greater than text length. index " + indexOfCursor + " range " + charsForward + " '" + text + "'");
		for (int index = indexOfCursor; index < indexOfCursor + charsForward; index++) {
			markers.add(index);
		}
		return this;
	}
	
	public List<Integer> markers() {
		return markers;
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
	
	public StringCursor firstMarker() {
		 currentMarker = 0;
		 return this;
	}
	
	public int indexOfCursor() {
		return indexOfCursor;
	}
	
	public int indexOfMarker() {
		if (!markers.isEmpty())
			return markers.get(currentMarker);
		return 0;
	}
	
	@Override
	public String toString() {
		return text;
	}
}
