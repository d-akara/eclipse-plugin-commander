package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import dakara.eclipse.plugin.stringscore.StringScore;

public class ListRankAndSelectorTest {
	@Test
	public void verifySelection() {
		List<ColumnOptions<String>> options = new ArrayList<>();
		options.add(new ColumnOptions<String>(item -> item));
		ListRankAndSelector<String> listRankAndSelector = new ListRankAndSelector<>(options, (filter) -> Stream.of("abc", "def", "ghi").collect(Collectors.toList()), StringScore::contains);
		List<KaviListItem<String>> listItems = listRankAndSelector.rankAndSelect("abc");
		Assert.assertEquals("abc", listItems.get(0).dataItem);
	}
	
	@Test
	public void verifyColumnSelection() {
		List<ColumnOptions<TestItem>> options = new ArrayList<>();
		options.add(new ColumnOptions<TestItem>(item -> item.field1));
		options.add(new ColumnOptions<TestItem>(item -> item.field2));
		
		List<TestItem> itemList = new ArrayList<>();
		itemList.add(new TestItem("1", "one"));
		itemList.add(new TestItem("2", "two"));
		itemList.add(new TestItem("3", "three"));
		
		ListRankAndSelector<TestItem> listRankAndSelector = new ListRankAndSelector<>(options, (filter) -> itemList, StringScore::contains);
		List<KaviListItem<TestItem>> listItems = listRankAndSelector.rankAndSelect("1");
		Assert.assertEquals("one", listItems.get(0).dataItem.field2);
	}
	
	@Test
	public void verifyColumnSelection2() {
		List<ColumnOptions<TestItem>> options = new ArrayList<>();
		options.add(new ColumnOptions<TestItem>(item -> item.field1));
		options.add(new ColumnOptions<TestItem>(item -> item.field2));
		
		List<TestItem> itemList = new ArrayList<>();
		itemList.add(new TestItem("1", "one"));
		itemList.add(new TestItem("2", "two"));
		itemList.add(new TestItem("3", "three"));
		
		ListRankAndSelector<TestItem> listRankAndSelector = new ListRankAndSelector<>(options, (filter) -> itemList, StringScore::contains);
		List<KaviListItem<TestItem>> listItems = listRankAndSelector.rankAndSelect("one");
		Assert.assertEquals("one", listItems.get(0).dataItem.field2);
	}
	
	@Test
	public void verifyColumnSelection3() {
		List<ColumnOptions<TestItem>> options = new ArrayList<>();
		options.add(new ColumnOptions<TestItem>(item -> item.field1));
		options.add(new ColumnOptions<TestItem>(item -> item.field2));
		
		List<TestItem> itemList = new ArrayList<>();
		itemList.add(new TestItem("1", "one"));
		itemList.add(new TestItem("2", "two"));
		itemList.add(new TestItem("3", "three"));
		
		ListRankAndSelector<TestItem> listRankAndSelector = new ListRankAndSelector<>(options, (filter) -> itemList, StringScore::contains);
		List<KaviListItem<TestItem>> listItems = listRankAndSelector.rankAndSelect("|two");
		Assert.assertEquals("2", listItems.get(0).dataItem.field1);
	}
	
	private class TestItem {
		public final String field1;
		public final String field2;
		
		public TestItem(String field1, String field2) {
			this.field1 = field1;
			this.field2 = field2;
		}
	}
}
