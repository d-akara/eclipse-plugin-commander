package dakara.eclipse.plugin.kavi.picklist;

import org.junit.Assert;
import org.junit.Test;


public class InputCommandTest {
	@Test
	public void singleFilterText() {
		InputCommand inputCommand = InputCommand.parse("abc");
		Assert.assertEquals("abc", inputCommand.getColumnFilterOptions(0).rawInputText);
	}
	@Test
	public void multiFilterText() {
		InputCommand inputCommand = InputCommand.parse("abc,def");
		Assert.assertEquals("abc", inputCommand.getColumnFilterOptions(0).rawInputText);
		Assert.assertEquals("def", inputCommand.getColumnFilterOptions(1).rawInputText);
	}
	@Test
	public void multiSelectFilterWithCommand() {
		InputCommand inputCommand = InputCommand.parse("abc,def//run");
		Assert.assertEquals("abc", inputCommand.getColumnFilterOptions(0).rawInputText);
		Assert.assertEquals("def", inputCommand.getColumnFilterOptions(1).rawInputText);
		Assert.assertEquals("run", inputCommand.fastSelectIndex);
	}
	@Test
	public void multiFilterWithSelect() {
		InputCommand inputCommand = InputCommand.parse("abc,def/zzz");
		Assert.assertEquals("abc", inputCommand.getColumnFilterOptions(0).rawInputText);
		Assert.assertEquals("def", inputCommand.getColumnFilterOptions(1).rawInputText);
		Assert.assertEquals("zzz", inputCommand.fastSelectIndex);
	}
	
	@Test
	public void noFilterFirstColumn() {
		InputCommand inputCommand = InputCommand.parse(",def");
		Assert.assertEquals("", inputCommand.getColumnFilterOptions(0).rawInputText);
		Assert.assertEquals("def", inputCommand.getColumnFilterOptions(1).rawInputText);
	}
	
	@Test
	public void filterLiteralMatching() {
		InputCommand inputCommand = InputCommand.parse("abc ");
		Assert.assertEquals("abc", inputCommand.getColumnFilterOptions(0).filterTextCursorPrimitive.asString());
		Assert.assertEquals(true, inputCommand.getColumnFilterOptions(0).scoreAsLiteral);
	}
	
	@Test
	public void filterLiteralMatchingWithNoInverseText() {
		InputCommand inputCommand = InputCommand.parse("abc!");
		Assert.assertEquals("abc", inputCommand.getColumnFilterOptions(0).filterTextCursorPrimitive.asString());
		Assert.assertEquals(false, inputCommand.getColumnFilterOptions(0).scoreAsLiteral);
	}
	
	@Test
	public void filterOptionsLiteralWithInverse() {
		InputCommand inputCommand = InputCommand.parse("abc !xyz");
		Assert.assertEquals("abc !xyz", inputCommand.getColumnFilterOptions(0).rawInputText);
		Assert.assertEquals("abc", inputCommand.getColumnFilterOptions(0).filterTextCursorPrimitive.asString());
		Assert.assertEquals(true, inputCommand.getColumnFilterOptions(0).inverseMatch);
	}
}
