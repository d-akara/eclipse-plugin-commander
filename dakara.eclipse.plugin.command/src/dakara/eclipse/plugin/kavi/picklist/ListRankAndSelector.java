package dakara.eclipse.plugin.kavi.picklist;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class ListRankAndSelector<T> {
	private Function<String, List<T>> listContentProvider;
	private List<ColumnOptions<T>> columnOptions;
	private BiFunction<String, String, Score> rankStringFn;
	
	public ListRankAndSelector(List<ColumnOptions<T>> columnOptions, Function<String, List<T>> listContentProvider, BiFunction<String, String, Score> rankStringFn) {
		this.listContentProvider = listContentProvider;
		this.columnOptions = columnOptions;
		this.rankStringFn = rankStringFn;
	}
	
	public List<KaviListItem<T>> rankAndSelect(String filter) {
		List<InputCommand> inputCommands = InputCommand.parse(filter);
		return listContentProvider.apply(filter).stream().
				       map(item -> new KaviListItem<>(item)).
				       peek(item -> {
				    	   AtomicInteger columnIndex = new AtomicInteger(0);
				    	   columnOptions.stream().map(options -> options.getColumnContentFn().apply(item.dataItem)).
				    	   		forEach(columnText -> item.addScore(rankStringFn.apply(columnText, inputCommands.get(0).getColumnFilter(columnIndex.getAndIncrement()))));  
				       }).
				       sorted((itemA, itemB) -> Integer.compare(itemB.totalScore(), itemA.totalScore())).
				       filter(item -> item.totalScore() > 0).
					   collect(Collectors.toList());
	}
}
