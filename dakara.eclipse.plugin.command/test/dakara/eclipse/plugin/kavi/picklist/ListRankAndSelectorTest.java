package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.RankedItem;
import dakara.eclipse.plugin.stringscore.StringScore;
import dakara.eclipse.plugin.stringscore.StringScoreRanking;

public class ListRankAndSelectorTest {
	private ListRankAndFilter<TestItem> rankSelectorMultiColumn = null;
	private List<TestItem> itemList = new ArrayList<>();
	@Before
	public void makeMultiColumnData() {
		itemList.add(new TestItem("1", "one",  "4"));
		itemList.add(new TestItem("2", "two",  "3"));
		itemList.add(new TestItem("3", "three","2"));
		itemList.add(new TestItem("4", "four", "1"));
		itemList.add(new TestItem("5", "abc def ghi", "xyz"));
		itemList.add(new TestItem("6", "abc def ghi", "abc"));
		itemList.add(new TestItem("7", "abc def ghi", "ghi"));
		itemList.add(new TestItem("8", "abc def ghi", "adg "));
		itemList.add(new TestItem("9", null,			 ""));
		itemList.add(new TestItem("0", "",			 ""));
		
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		rankSelectorMultiColumn = new ListRankAndFilter<>(stringScore::scoreCombination, item -> item.field1);
		rankSelectorMultiColumn.addField("f1", item -> item.field1);
		rankSelectorMultiColumn.addField("f2", item -> item.field2);
		rankSelectorMultiColumn.addField("f3", item -> item.field3);
	}
	
	@Test
	public void verifyColumn1Selection() {
		InputCommand inputCommand = InputCommand.parse("1");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		Assert.assertEquals("one",  listItems.get(0).dataItem.field2);
		Assert.assertEquals("four", listItems.get(1).dataItem.field2);
	}
	
	@Test
	public void verifyColumn2Selection2() {
		InputCommand inputCommand = InputCommand.parse("one");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		Assert.assertEquals("one", listItems.get(0).dataItem.field2);
	}
	
	@Test
	public void verifyColumn2OnlySelection() {
		InputCommand inputCommand = InputCommand.parse(",two");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		Assert.assertEquals("2", listItems.get(0).dataItem.field1);
	}
	
	@Test
	public void verifyColumnSelection3() {
		InputCommand inputCommand = InputCommand.parse(",,3");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		Assert.assertEquals("2", listItems.get(0).dataItem.field1);
	}
	
	@Test
	public void multipleWordsOutOfOrder() {
		InputCommand inputCommand = InputCommand.parse("def abc");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		RankedItem<TestItem> listItem = listItems.get(0);
		Assert.assertEquals("5", listItem.dataItem.field1);
		Assert.assertEquals(6, (listItem.getColumnScore("f2").matches.size()));
	}
	
	@Test
	public void spaceAtEndShouldNotMatch() {
		InputCommand inputCommand = InputCommand.parse("xyz ");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		RankedItem<TestItem> listItem = listItems.get(0);
		Assert.assertEquals(1, listItems.size());
		Assert.assertEquals(3, (listItem.getColumnScore("f3").matches.size()));
	}
	
	@Test
	public void spaceAtEndShouldNotMatch2() {
		InputCommand inputCommand = InputCommand.parse("adg ");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		RankedItem<TestItem> listItem = listItems.get(0);
		Assert.assertEquals("8", listItem.dataItem.field1);
		Assert.assertEquals(3, (listItem.getColumnScore("f3").matches.size()));
	}
	
	@Test
	public void spaceAtEndForceLiteralMatching() {
		InputCommand inputCommand = InputCommand.parse("ad");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		Assert.assertEquals(4, listItems.size());
		
		inputCommand = InputCommand.parse("ad ");
		listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		Assert.assertEquals(1, listItems.size());
	}
	
	@Test
	public void findMatchTrailingEmptyColumns() {
		InputCommand inputCommand = InputCommand.parse("0");
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		RankedItem<TestItem> listItem = listItems.get(0);
		Assert.assertEquals("0", listItem.dataItem.field1);
		Assert.assertEquals(0, (listItem.getColumnScore("f3").matches.size()));
		Assert.assertEquals(0, (listItem.getColumnScore("f2").matches.size()));
		Assert.assertEquals(1, (listItem.getColumnScore("f1").matches.size()));
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
