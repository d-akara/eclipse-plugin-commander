package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.kavi.picklist.ColumnOptions;
import dakara.eclipse.plugin.kavi.picklist.InputCommand;
import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class ListRankAndFilter<T> {
	private Function<InputCommand, List<T>> listContentProvider;
	private List<ColumnOptions<T>> columnOptions;
	private BiFunction<String, String, Score> rankingStrategy;
	private Function<T, String> sortFieldResolver;
	
	// TODO make generic list and filter not dependent on any UI
	public ListRankAndFilter(List<ColumnOptions<T>> columnOptions, Function<InputCommand, List<T>> listContentProvider, BiFunction<String, String, Score> rankingStrategy, Function<T, String> sortFieldResolver) {
		this.listContentProvider = listContentProvider;
		this.columnOptions = columnOptions.stream().filter(option -> option.isSearchable()).collect(Collectors.toList());
		this.rankingStrategy = rankingStrategy;
		this.sortFieldResolver = sortFieldResolver;
	}
	
	public List<RankedItem<T>> rankAndFilter(final InputCommand inputCommand) {
		return listContentProvider.apply(inputCommand).parallelStream().
				       map(item -> new RankedItem<>(item)).
				       map(item -> setItemRank(item, inputCommand)).
				       filter(item -> item.totalScore() > 0).
				       sorted(Comparator.comparing((RankedItem item) -> item.totalScore()).reversed().thenComparing(item -> sortFieldResolver.apply((T) item.dataItem))).
					   collect(Collectors.toList());
	}
	
	private RankedItem<T> setItemRank(RankedItem<T> rankedItem, final InputCommand inputCommand) {
		rankedItem.setScoreModeByColumn(inputCommand.isColumnFiltering);
		
		if (inputCommand.isColumnFiltering) {
			int searchableColumnCount = 0;
			for (ColumnOptions<T> options : columnOptions) {
				rankedItem.addScore(rankingStrategy.apply(inputCommand.getColumnFilter(searchableColumnCount), options.getColumnContentFn().apply(rankedItem.dataItem, -1)), options.columnId);
				searchableColumnCount++;
			} 
		} else {
			List<Score> scores = scoreAllAsOneColumn(rankedItem, inputCommand);
			for (ColumnOptions<T> options : columnOptions) {
				rankedItem.addScore(scores.remove(0), options.columnId);
			} 
		}
		return rankedItem;
	}
	
	private List<Score> scoreAllAsOneColumn(RankedItem<T> listItem, final InputCommand inputCommand) {
		List<Integer> indexesOfColumnBreaks = new ArrayList<>();
		StringBuilder allColumnText = new StringBuilder();
		buildAllColumnTextAndIndexes(listItem, indexesOfColumnBreaks, allColumnText);
		
		Score allColumnScore = rankingStrategy.apply( inputCommand.getColumnFilter(0), allColumnText.toString());
		if (allColumnScore.rank > 0) {
			return convertScoreToMatchesPerColumn(allColumnText.toString(), allColumnScore, indexesOfColumnBreaks);
		} else {
			// There was no match.  Add the empty to score to all columns
			List<Score> scores = new ArrayList<>();
			for (ColumnOptions<T> options : columnOptions) {
				scores.add(allColumnScore);
			} 
			return scores;
		}
	}

	private void buildAllColumnTextAndIndexes(RankedItem<T> listItem, List<Integer> indexesOfColumnBreaks, StringBuilder allColumnText) {
		for (ColumnOptions<T> column : columnOptions) {
			if (!column.isSearchable()) continue;
			
			String columnContent = column.getColumnContentFn().apply(listItem.dataItem, -1);
			allColumnText.append(columnContent).append(" ");
			indexesOfColumnBreaks.add(allColumnText.length() - 1);
		}
	}	
	
	private List<Score> convertScoreToMatchesPerColumn(String originalText, Score allColumnScore, List<Integer> indexesOfColumnBreaks) {
		List<Score> scores = new ArrayList<>();
		List<Integer> matches = new ArrayList<>();
		int offset = 0;
		
		for (int index = 0; index < originalText.length(); index++) {
			if (allColumnScore.matches.size() > 0 && index == allColumnScore.matches.get(0)) {
				allColumnScore.matches.remove(0);
				matches.add(index - offset);
			}
			
			if (index == indexesOfColumnBreaks.get(0) || index == originalText.length() - 1) {
				indexesOfColumnBreaks.remove(0);
				scores.add(new Score(allColumnScore.rank, matches));
				matches = new ArrayList<>();
				offset += index + 1;
			}
		}
		
		return scores;
	}
}
