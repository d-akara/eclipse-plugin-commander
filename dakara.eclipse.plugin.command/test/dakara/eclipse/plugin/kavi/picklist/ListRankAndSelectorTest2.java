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

public class ListRankAndSelectorTest2 {
	private ListRankAndFilter<TestItem> rankSelectorMultiColumn = null;
	private List<TestItem> itemList = new ArrayList<>();
	@Before
	public void makeMultiColumnData() {
		itemList.add(new TestItem("1", "WizardSnow",         "4"));
		itemList.add(new TestItem("2", "AbstractWizard.js",  "4"));
		itemList.add(new TestItem("3", "one",                "4"));
		

		
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		rankSelectorMultiColumn = new ListRankAndFilter<>(stringScore::scoreCombination, item -> item.field1);
		rankSelectorMultiColumn.addField("f1", item -> item.field1);
		rankSelectorMultiColumn.addField("f2", item -> item.field2);
		rankSelectorMultiColumn.addField("f3", item -> item.field3);
	}
	
	@Test
	public void verifyColumn1Selection() {
		InputCommand inputCommand = InputCommand.parse("|wizard|").get(0);
		List<RankedItem<TestItem>> listItems = rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		Assert.assertEquals("1",  listItems.get(0).dataItem.field1);
		Assert.assertEquals("2", listItems.get(1).dataItem.field1);
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
