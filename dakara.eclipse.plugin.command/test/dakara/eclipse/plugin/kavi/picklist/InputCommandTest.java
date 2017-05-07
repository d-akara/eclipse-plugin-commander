package dakara.eclipse.plugin.kavi.picklist;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;


public class InputCommandTest {
	@Test
	public void singleFilterText() {
		List<InputCommand> inputCommand = InputCommand.parse("abc");
		Assert.assertEquals("abc", inputCommand.get(0).getColumnFilter(0));
	}
	@Test
	public void multiFilterText() {
		List<InputCommand> inputCommand = InputCommand.parse("abc|def");
		Assert.assertEquals("abc", inputCommand.get(0).getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.get(0).getColumnFilter(1));
	}
	@Test
	public void multiFilterWithCommand() {
		List<InputCommand> inputCommand = InputCommand.parse("abc|def:run");
		Assert.assertEquals("abc", inputCommand.get(0).getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.get(0).getColumnFilter(1));
		Assert.assertEquals("run", inputCommand.get(1).getColumnFilter(0));
	}
	@Test
	public void multiFilterWithSelect() {
		List<InputCommand> inputCommand = InputCommand.parse("abc|def/zzz");
		Assert.assertEquals("abc", inputCommand.get(0).getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.get(0).getColumnFilter(1));
		Assert.assertEquals("zzz", inputCommand.get(0).fastSelectIndex);
	}
	
	@Test
	public void noFilterFirstColumn() {
		List<InputCommand> inputCommand = InputCommand.parse("|def");
		Assert.assertEquals("", inputCommand.get(0).getColumnFilter(0));
		Assert.assertEquals("def", inputCommand.get(0).getColumnFilter(1));
	}
}
