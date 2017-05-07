package dakara.eclipse.plugin.kavi.picklist;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class ListRankAndFilter<T> {
	private Function<InputCommand, List<T>> listContentProvider;
	private List<ColumnOptions<T>> columnOptions;
	private BiFunction<String, String, Score> rankingStrategy;
	
	public ListRankAndFilter(List<ColumnOptions<T>> columnOptions, Function<InputCommand, List<T>> listContentProvider, BiFunction<String, String, Score> rankingStrategy) {
		this.listContentProvider = listContentProvider;
		this.columnOptions = columnOptions;
		this.rankingStrategy = rankingStrategy;
	}
	
	public List<KaviListItem<T>> rankAndFilter(final InputCommand inputCommand) {
		return listContentProvider.apply(inputCommand).stream().
				       map(item -> new KaviListItem<>(item)).
				       peek(item -> {
				    	   AtomicInteger columnIndex = new AtomicInteger(0);
				    	   columnOptions.stream().map(options -> options.getColumnContentFn().apply(item.dataItem)).
				    	   		forEach(columnText -> item.addScore(rankingStrategy.apply(columnText, inputCommand.getColumnFilter(columnIndex.getAndIncrement()))));  
				       }).
				       sorted((itemA, itemB) -> Integer.compare(itemB.totalScore(), itemA.totalScore())).
				       filter(item -> item.totalScore() > 0).
					   collect(Collectors.toList());
	}
}
