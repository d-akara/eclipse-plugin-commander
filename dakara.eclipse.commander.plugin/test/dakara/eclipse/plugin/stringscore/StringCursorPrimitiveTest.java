package dakara.eclipse.plugin.stringscore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringCursorPrimitiveTest {
	@Test
	public void initialCursorPositionAtStart() {
		StringCursorPrimitive cursor = new StringCursorPrimitive("openFile TestCase first-second next.LastTime a * - + word");
		
		System.out.println(cursor.toString());
	}
	
	
	@Test
	public void singleNumber() {
		StringCursorPrimitive cursor = new StringCursorPrimitive("3 45 time2 4wheel two2two openFile2 open2File");
		
		System.out.println(cursor.toString());
	}
	
	@Test
	public void indexOf() {
		StringCursorPrimitive cursor = new StringCursorPrimitive("openFile TestCase first-second next.LastTime a * - + word");
		assertEquals(18, cursor.indexOf("first".toCharArray(), 0));
	}
}
