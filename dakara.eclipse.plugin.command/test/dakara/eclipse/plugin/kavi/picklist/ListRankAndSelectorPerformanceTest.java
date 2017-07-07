package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import dakara.eclipse.plugin.stringscore.ListRankAndFilter;
import dakara.eclipse.plugin.stringscore.StringScore;
import dakara.eclipse.plugin.stringscore.StringScoreRanking;

public class ListRankAndSelectorPerformanceTest {
	private ListRankAndFilter<TestItem> rankSelectorMultiColumn = null;
	private List<TestItem> itemList = new ArrayList<>();
	@Before
	public void makeMultiColumnData() {
		for (int i = 0; i < 100000; i++) {
			itemList.add(new TestItem("1", "one",  UUID.randomUUID().toString()));
			itemList.add(new TestItem("0", "abc def ghi jkl mno pqr stu vwx yz",			 "one two three four five six seven eight nine ten"));
		}
		
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		rankSelectorMultiColumn = new ListRankAndFilter<>(stringScore::scoreCombination, item -> item.field1);
		rankSelectorMultiColumn.addField("f1", item -> item.field1);
		rankSelectorMultiColumn.addField("f2", item -> item.field2);
		rankSelectorMultiColumn.addField("f3", item -> item.field3);
	}

	@Test
	public void verifyColumn1Selection() {
		// 100 iterations currently ~ 13.5 ~ 14.5secs
		for (int i = 0; i < 100; i++) {			
			InputCommand inputCommand = InputCommand.parse("defmno").get(0);
			rankSelectorMultiColumn.rankAndFilter(inputCommand, itemList);
		}
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
