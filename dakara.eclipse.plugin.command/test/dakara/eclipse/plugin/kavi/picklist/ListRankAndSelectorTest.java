package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dakara.eclipse.plugin.stringscore.StringScore;

public class ListRankAndSelectorTest {
	private ListRankAndFilter<TestItem> rankSelectorMultiColumn = null;
	@Before
	public void makeMultiColumnData() {
		List<ColumnOptions<TestItem>> options = new ArrayList<>();
		options.add(new ColumnOptions<TestItem>(item -> item.field1));
		options.add(new ColumnOptions<TestItem>(item -> item.field2));
		options.add(new ColumnOptions<TestItem>(item -> item.field3));
		
		List<TestItem> itemList = new ArrayList<>();
		itemList.add(new TestItem("1", "one",  "4"));
		itemList.add(new TestItem("2", "two",  "3"));
		itemList.add(new TestItem("3", "three","2"));
		itemList.add(new TestItem("4", "four", "1"));
		
		rankSelectorMultiColumn = new ListRankAndFilter<>(options, (filter) -> itemList, StringScore::contains);
	}
	
	@Test
	public void verifyColumn1Selection() {
		InputCommand inputCommand = InputCommand.parse("1").get(0);
		List<KaviListItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand);
		Assert.assertEquals("one",  listItems.get(0).dataItem.field2);
		Assert.assertEquals("four", listItems.get(1).dataItem.field2);
	}
	
	@Test
	public void verifyColumn2Selection2() {
		InputCommand inputCommand = InputCommand.parse("one").get(0);
		List<KaviListItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand);
		Assert.assertEquals("one", listItems.get(0).dataItem.field2);
	}
	
	@Test
	public void verifyColumn2OnlySelection() {
		InputCommand inputCommand = InputCommand.parse("|two").get(0);
		List<KaviListItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand);
		Assert.assertEquals("2", listItems.get(0).dataItem.field1);
	}
	
	@Test
	public void verifyColumnSelection3() {
		InputCommand inputCommand = InputCommand.parse("||3").get(0);
		List<KaviListItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand);
		Assert.assertEquals("2", listItems.get(0).dataItem.field1);
	}
	
	private class TestItem {
		public final String field1;
		public final String field2;
		public final String field3;
		
		public TestItem(String field1, String field2, String field3) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
		}
	}
}
