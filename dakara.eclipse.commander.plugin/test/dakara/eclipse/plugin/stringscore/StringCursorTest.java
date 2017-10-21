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
	public void verifyWordBoundaries() {
		StringCursor cursor = new StringCursor("abc def ghi2");
		assertEquals('c', cursor.moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('c', cursor.moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('a', cursor.moveCursorPreviousPartialWordStart().currentChar());
		assertEquals('a', cursor.moveCursorPreviousPartialWordStart().currentChar());
		
		assertEquals('f', cursor.setCursorPosition(5).moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('f', cursor.moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('d', cursor.moveCursorPreviousPartialWordStart().currentChar());
		assertEquals('d', cursor.moveCursorPreviousPartialWordStart().currentChar());
		
		assertEquals('i', cursor.setCursorPosition(9).moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('i', cursor.moveCursorForwardPartialWordEnd().currentChar());
	}
	
	@Test
	public void verifyWordBoundariesDigits() {
		StringCursor cursor = new StringCursor("123 456 789");
		assertEquals('3', cursor.moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('3', cursor.moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('1', cursor.moveCursorPreviousPartialWordStart().currentChar());
		assertEquals('1', cursor.moveCursorPreviousPartialWordStart().currentChar());
		
		assertEquals('6', cursor.setCursorPosition(5).moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('6', cursor.moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('4', cursor.moveCursorPreviousPartialWordStart().currentChar());
		assertEquals('4', cursor.moveCursorPreviousPartialWordStart().currentChar());
		
		assertEquals('9', cursor.setCursorPosition(9).moveCursorForwardPartialWordEnd().currentChar());
		assertEquals('9', cursor.moveCursorForwardPartialWordEnd().currentChar());
	}
	
	@Test
	public void verifyWordAtCursor() {
		StringCursor cursor = new StringCursor("abc def ghi");
		assertEquals("", cursor.setCursorPosition(-1).wordAtCursor());
		assertEquals(-1, cursor.indexOfCursor());
		
		assertEquals("abc", cursor.setCursorPosition(0).wordAtCursor());
		assertEquals(0, cursor.indexOfCursor());
		assertEquals("abc", cursor.setCursorPosition(1).wordAtCursor());
		assertEquals(1, cursor.indexOfCursor());
		assertEquals("abc", cursor.setCursorPosition(2).wordAtCursor());
		assertEquals(2, cursor.indexOfCursor());
		
		assertEquals("abc", cursor.setCursorPosition(3).wordAtCursor());
		assertEquals(3, cursor.indexOfCursor());
		
		assertEquals("def", cursor.setCursorPosition(4).wordAtCursor());
		assertEquals("def", cursor.setCursorPosition(5).wordAtCursor());
		assertEquals("def", cursor.setCursorPosition(6).wordAtCursor());
		
		assertEquals("ghi", cursor.setCursorPosition(8).wordAtCursor());
		assertEquals("ghi", cursor.setCursorPosition(9).wordAtCursor());
		assertEquals("ghi", cursor.setCursorPosition(10).wordAtCursor());
		
		assertEquals("", cursor.setCursorPosition(11).wordAtCursor());
	}
	
	@Test
	public void verifyMarkerPositions() {
		StringCursor cursor = new StringCursor("abc def ghi");
		cursor.addMark(0).addMark(2);
		assertEquals('a', cursor.currentMarker());
		assertEquals('c', cursor.setNextMarkCurrent().currentMarker());
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
		cursor.addMark(1).addMark(10);
		assertEquals(1, cursor.countUnMarkedWordsBetweenMarkers(0, 1));
		
		cursor.clearMarkers().addMark(1).addMark(22);
		assertEquals(4, cursor.countUnMarkedWordsBetweenMarkers(0, 1));
		
		cursor.clearMarkers().addMark(0).addMark(12);
		assertEquals(2, cursor.countUnMarkedWordsBetweenMarkers(0, 1));
	}
	
	@Test
	public void verifyPartialWordGaps() {
		StringCursor cursor = new StringCursor("abcDef ghi abc def ghi");
		cursor.addMark(1).addMark(10);
		assertEquals(2, cursor.countPartialWordsBetweenMarkers(0, 1));
		
		cursor.clearMarkers().addMark(1).addMark(21);
		assertEquals(5, cursor.countPartialWordsBetweenMarkers(0, 1));

		cursor.clearMarkers().addMark(0).addMark(12);
		assertEquals(3, cursor.countPartialWordsBetweenMarkers(0, 1));
	}
	
	@Test
	public void verifyOrderOfMarks() {
		StringCursor cursor = new StringCursor("abc def ghi abc def ghi");
		cursor.addMark(1).addMark(10);
		assertEquals(1, cursor.countUnMarkedWordsBetweenMarkers(0, 1));
		
		cursor.clearMarkers().addMark(10).addMark(1);
		assertEquals(1, cursor.countUnMarkedWordsBetweenMarkers(0, 1));
		
		cursor.clearMarkers().setCursorPosition(4).markFillRangeForward(3).setCursorPosition(0).markFillRangeForward(3);
		assertEquals("abcdef", cursor.markersAsString());
		
		cursor.clearMarkers().setCursorPosition(0).markFillRangeForward(3).setCursorPosition(4).markFillRangeForward(3);
		assertEquals("abcdef", cursor.markersAsString());
	}
	
	//@Test
	public void verifyMoveCursorIndexAlphaSequence() {
		StringCursor cursor = new StringCursor("abc def ghi abc def ghi");
		cursor.moveCursorForwardIndexOfAlphaSequence("fgh");
		assertEquals(6, cursor.indexOfCursor());
		
		cursor = new StringCursor("fabc def ghi abc def ghi");
		cursor.moveCursorForwardIndexOfAlphaSequence("fgh");
		assertEquals(7, cursor.indexOfCursor());
		
		cursor = new StringCursor("fabc def ghi abc def ghi");
		cursor.moveCursorForwardIndexOfAlphaSequence("fgha");
		assertEquals(-1, cursor.indexOfCursor());
	}
	
	@Test
	public void verifyCamelCaseWordSupport() {
		StringCursor cursor = new StringCursor("openFileName testing longList");
		cursor.moveCursorForwardPartialWordStart();
		assertEquals(0, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(4, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(8, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(13, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(21, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(25, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(true, cursor.cursorPositionTerminal());
		
	}
	
	@Test
	public void verifyCamelCaseWordSupport2() {
		StringCursor cursor = new StringCursor("IHTTPClient IActionBar");
		cursor.moveCursorForwardPartialWordStart();
		assertEquals(0, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(5, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(12, cursor.indexOfCursor());
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(13, cursor.indexOfCursor());
		
		cursor.moveCursorForward().moveCursorForwardPartialWordStart();
		assertEquals(19, cursor.indexOfCursor());
	}
	
	@Test
	public void verifyPartialWordSupport() {
		StringCursor cursor = new StringCursor("openFileName testing longList");
		cursor.setCursorPosition(2).moveCursorForwardPartialWordEnd();
		assertEquals(3, cursor.indexOfCursor());
		
		cursor.setCursorPosition(2).moveCursorPreviousPartialWordStart();
		assertEquals(0, cursor.indexOfCursor());
		
		cursor.setCursorPosition(2).moveCursorForwardPartialWordStart();
		assertEquals(4, cursor.indexOfCursor());
		
		cursor.setCursorPosition(15).moveCursorForwardPartialWordEnd();
		assertEquals(19, cursor.indexOfCursor());
		
		cursor.setCursorPosition(15).moveCursorPreviousPartialWordStart();
		assertEquals(13, cursor.indexOfCursor());
		
		cursor.setCursorPosition(15).moveCursorForwardPartialWordStart();
		assertEquals(21, cursor.indexOfCursor());
		
		assertEquals("file", cursor.setCursorPosition(5).partialWordAtCursor());
		assertEquals("testing", cursor.setCursorPosition(15).partialWordAtCursor());
		assertEquals("name", cursor.setCursorPosition(8).partialWordAtCursor());

	}
}
