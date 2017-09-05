package dakara.eclipse.plugin.stringscore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringCursorPrimitiveTest {
	@Test
	public void initialCursorPositionAtStart() {
		StringCursorPrimitive cursor = new StringCursorPrimitive("openFile TestCase first-second next.LastTime a * - + word");
		
		System.out.println(cursor.toString());
	}
}
