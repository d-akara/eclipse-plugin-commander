package dakara.eclipse.plugin.kavi.picklist;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class ListRankAndFilter<T> {
	private Function<InputCommand, List<T>> listContentProvider;
	private List<ColumnOptions<T>> columnOptions;
	private BiFunction<String, String, Score> rankingStrategy;
	private Function<T, String> sortFieldResolver;
	
	// TODO make generic list and filter not dependent on any UI
	public ListRankAndFilter(List<ColumnOptions<T>> columnOptions, Function<InputCommand, List<T>> listContentProvider, BiFunction<String, String, Score> rankingStrategy, Function<T, String> sortFieldResolver) {
		this.listContentProvider = listContentProvider;
		this.columnOptions = columnOptions;
		this.rankingStrategy = rankingStrategy;
		this.sortFieldResolver = sortFieldResolver;
	}
	
	public List<KaviListItem<T>> rankAndFilter(final InputCommand inputCommand) {
		return listContentProvider.apply(inputCommand).parallelStream().
				       map(item -> new KaviListItem<>(item)).
				       map(item -> setItemRank(item, inputCommand)).
				       filter(item -> item.totalScore() > 0).
				       sorted(Comparator.comparing((KaviListItem item) -> item.totalScore()).reversed().thenComparing(item -> sortFieldResolver.apply((T) item.dataItem))).
					   collect(Collectors.toList());
	}
	
	// TODO generic way to determine field filters vs inputCommand
	// map inputCommand filters to column id's (index)
	private KaviListItem<T> setItemRank(KaviListItem<T> listItem, final InputCommand inputCommand) {
		listItem.setScoreModeAllRequired(!inputCommand.filterAnyColumn);
		columnOptions.stream()	
			.filter(options -> options.isSearchable())
			// TODO need to change the column index - 1 which takes into account the alpha index column
			.forEach(options -> listItem.addScore(rankingStrategy.apply(options.getColumnContentFn().apply(listItem.dataItem, -1), inputCommand.getColumnFilter(options.getColumnIndex() - 1)), options.getColumnIndex())); 
		return listItem;
	}
}
