package dakara.eclipse.plugin.kavi.picklist;

import org.junit.Assert;
import org.junit.Test;


public class InputCommandTest {
	@Test
	public void singleFilterText() {
		InputCommand inputCommand = InputCommand.parse("abc");
		Assert.assertEquals("abc", inputCommand.getColumnFilter(0));
	}
	@Test
	public void multiFilterText() {
		InputCommand inputCommand = InputCommand.parse("abc,def");
		Assert.assertEquals("abc", inputCommand.getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.getColumnFilter(1));
	}
	@Test
	public void multiSelectFilterWithCommand() {
		InputCommand inputCommand = InputCommand.parse("abc,def//run");
		Assert.assertEquals("abc", inputCommand.getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.getColumnFilter(1));
		Assert.assertEquals("run", inputCommand.fastSelectIndex);
	}
	@Test
	public void multiFilterWithSelect() {
		InputCommand inputCommand = InputCommand.parse("abc,def/zzz");
		Assert.assertEquals("abc", inputCommand.getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.getColumnFilter(1));
		Assert.assertEquals("zzz", inputCommand.fastSelectIndex);
	}
	
	@Test
	public void noFilterFirstColumn() {
		InputCommand inputCommand = InputCommand.parse(",def");
		Assert.assertEquals("", inputCommand.getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.getColumnFilter(1));
	}
}
