package dakara.eclipse.plugin.stringscore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringCursorTest {
	@Test
	public void initialCursorPositionAtStart() {
		StringCursor cursor = new StringCursor("abc");
		assertEquals('a', cursor.currentChar());
	}
	
	@Test
	public void moveCursorForwardOnePosition() {
		StringCursor cursor = new StringCursor("abc");
		assertEquals('b', cursor.moveCursorForward().currentChar());
	}
	
	@Test
	public void verifyIndexOfCursorMovement() {
		StringCursor cursor = new StringCursor("abc");
		assertEquals(0, cursor.indexOfCursor());
		assertEquals(1, cursor.moveCursorForward().indexOfCursor());
		assertEquals(2, cursor.moveCursorForward().indexOfCursor());
		assertEquals(3, cursor.moveCursorForward().indexOfCursor());
		assertEquals(2, cursor.moveCursorBackward().indexOfCursor());
		assertEquals(1, cursor.moveCursorBackward().indexOfCursor());
		assertEquals(0, cursor.moveCursorBackward().indexOfCursor());
		assertEquals(-1, cursor.moveCursorBackward().indexOfCursor());
	}
	
	@Test
	public void verifyIndexOfCursorMovementBySearch() {
		StringCursor cursor = new StringCursor("abc def ghi");
		assertEquals(5, cursor.moveCursorIndexOf("ef").indexOfCursor());
	}
	
	@Test
	public void verifyTerminalPositions() {
		StringCursor cursor = new StringCursor("abc");
		assertEquals(false, cursor.cursorPositionTerminal());
		assertEquals(true, cursor.moveCursorBackward().cursorPositionTerminal());
		assertEquals(true, cursor.setCursorPosition(3).cursorPositionTerminal());
		assertEquals(false, cursor.moveCursorBackward().cursorPositionTerminal());
	}
	
	@Test
	public void verifyAlphaBoundaries() {
		StringCursor cursor = new StringCursor("abc def ghi");
		assertEquals('c', cursor.moveCursorNextAlphaBoundary().currentChar());
		assertEquals('c', cursor.moveCursorNextAlphaBoundary().currentChar());
		assertEquals('a', cursor.moveCursorPreviousAlphaBoundary().currentChar());
		assertEquals('a', cursor.moveCursorPreviousAlphaBoundary().currentChar());
		
		assertEquals('f', cursor.setCursorPosition(5).moveCursorNextAlphaBoundary().currentChar());
		assertEquals('f', cursor.moveCursorNextAlphaBoundary().currentChar());
		assertEquals('d', cursor.moveCursorPreviousAlphaBoundary().currentChar());
		assertEquals('d', cursor.moveCursorPreviousAlphaBoundary().currentChar());
	}
	
	@Test
	public void verifyWordAtCursor() {
		StringCursor cursor = new StringCursor("abc def ghi");
		assertEquals("abc", cursor.setCursorPosition(0).wordAtCursor());
		assertEquals(0, cursor.indexOfCursor());
		assertEquals("abc", cursor.setCursorPosition(1).wordAtCursor());
		assertEquals(1, cursor.indexOfCursor());
		assertEquals("abc", cursor.setCursorPosition(2).wordAtCursor());
		assertEquals(2, cursor.indexOfCursor());
		
		assertEquals("def", cursor.setCursorPosition(4).wordAtCursor());
		assertEquals("def", cursor.setCursorPosition(5).wordAtCursor());
		assertEquals("def", cursor.setCursorPosition(6).wordAtCursor());
		
		assertEquals("ghi", cursor.setCursorPosition(8).wordAtCursor());
		assertEquals("ghi", cursor.setCursorPosition(9).wordAtCursor());
		assertEquals("ghi", cursor.setCursorPosition(10).wordAtCursor());
	}
	
	@Test
	public void verifyMarkerPositions() {
		StringCursor cursor = new StringCursor("abc def ghi");
		cursor.addMarker(0).addMarker(2);
		assertEquals('a', cursor.currentMarker());
		assertEquals('c', cursor.nextMarker().currentMarker());
	}
	
	@Test
	public void verifyMarkRange() {
		StringCursor cursor = new StringCursor("abc def ghi");
		assertEquals("def", cursor.moveCursorIndexOf("def").markFillRangeForward(3).markersAsString());
		
		cursor.clearMarkers();
		assertEquals("i", cursor.moveCursorIndexOf("i").markFillRangeForward(1).markersAsString());
	}
	
	@Test
	public void verifyWordGaps() {
		StringCursor cursor = new StringCursor("abc def ghi abc def ghi");
		cursor.addMarker(1).addMarker(10);
		assertEquals(1, cursor.wordGapsBetweenMarkedRegions(0, 1));
		
		cursor.clearMarkers().addMarker(1).addMarker(22);
		assertEquals(4, cursor.wordGapsBetweenMarkedRegions(0, 1));

		cursor.clearMarkers().addMarker(0).addMarker(12);
		assertEquals(2, cursor.wordGapsBetweenMarkedRegions(0, 1));
		
		
	}
	
}
