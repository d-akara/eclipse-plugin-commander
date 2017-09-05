package dakara.eclipse.plugin.stringscore;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringCursorPrimitiveTest {
	@Test
	public void initialCursorPositionAtStart() {
		StringCursorPrimitive cursor = new StringCursorPrimitive("openFile TestCase");
		
		System.out.println(cursor.toString());
	}
}
