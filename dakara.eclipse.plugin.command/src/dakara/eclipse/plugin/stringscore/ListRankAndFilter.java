package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import dakara.eclipse.plugin.kavi.picklist.InputCommand;
import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class ListRankAndFilter<T> {
	private List<FieldResolver<T>> fields = new ArrayList<>();
	private BiFunction<String, String, Score> rankingStrategy;
	private Function<T, String> sortFieldResolver;
	
	public ListRankAndFilter(BiFunction<String, String, Score> rankingStrategy, Function<T, String> sortFieldResolver) {
		this.rankingStrategy = rankingStrategy;
		this.sortFieldResolver = sortFieldResolver;
	}
	
	public static <T> ListRankAndFilter<T> make(Function<T, String> sortFieldResolver) {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		return new ListRankAndFilter<>(
				(filter, columnText) -> stringScore.scoreCombination(filter, columnText),
				sortFieldResolver);
	}
	
	public ListRankAndFilter<T> addField(String fieldId, Function<T, String> fieldResolver) {
		fields.add(new FieldResolver<>(fieldId, fieldResolver));
		return this;
	}
	
	public List<RankedItem<T>> rankAndFilter(final InputCommand inputCommand, List<T> items) {
		if (!inputCommand.isColumnFiltering && inputCommand.getColumnFilter(0).length() == 0) return makeRankedList(items);
		
		return items.parallelStream().
				       map(item -> new RankedItem<>(item)).
				       map(item -> setItemRank(item, inputCommand)).
				       filter(item -> item.totalScore() > 0).
				       sorted(Comparator.comparing((RankedItem item) -> item.totalScore()).reversed().thenComparing(item -> sortFieldResolver.apply((T) item.dataItem))).
					   collect(Collectors.toList());
	}
	
	public List<RankedItem<T>> moveItem(List<RankedItem<T>> itemList, T item, int order) {
		List<RankedItem<T>> filteredList = itemList.stream().filter(listItem -> !listItem.getDataItem().equals(item)).collect(Collectors.toList());
		filteredList.add(order, new RankedItem<T>(item));
		return filteredList;
	}
	
	private List<RankedItem<T>> makeRankedList(List<T> items) {
		return items.parallelStream().
	       map(item -> new RankedItem<>(item)).
	       sorted(Comparator.comparing((RankedItem item) -> item.totalScore()).reversed().thenComparing(item -> sortFieldResolver.apply((T) item.dataItem))).
		   collect(Collectors.toList());
	}
	
	private RankedItem<T> setItemRank(RankedItem<T> rankedItem, final InputCommand inputCommand) {
		rankedItem.setScoreModeByColumn(inputCommand.isColumnFiltering);
		
		if (inputCommand.isColumnFiltering) {
			int searchableColumnCount = 0;
			for (FieldResolver<T> field : fields) {
				rankedItem.addScore(rankingStrategy.apply(inputCommand.getColumnFilter(searchableColumnCount), field.fieldResolver.apply(rankedItem.dataItem)), field.fieldId);
				searchableColumnCount++;
			} 
		} else {
			List<Score> scores = scoreAllAsOneColumn(rankedItem, inputCommand);
			for (FieldResolver<T> field : fields) {
				rankedItem.addScore(scores.remove(0), field.fieldId);
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
			for (FieldResolver<T> field : fields) {
				scores.add(allColumnScore);
			} 
			return scores;
		}
	}

	/*
	 * concatenate all columns together with space separators.
	 * create list of index's where columns were joined
	 */
	private void buildAllColumnTextAndIndexes(RankedItem<T> listItem, List<Integer> indexesOfColumnBreaks, StringBuilder allColumnText) {
		for (int index = 0; index < fields.size(); index++) {
			FieldResolver<T> column = fields.get(index);
			String columnContent = column.fieldResolver.apply(listItem.dataItem);
			allColumnText.append(columnContent);
			if (index < fields.size() - 1) allColumnText.append(" ");
			indexesOfColumnBreaks.add(allColumnText.length() - 1);
		}
	}	
	
	private List<Score> convertScoreToMatchesPerColumn(String originalText, Score allColumnScore, List<Integer> indexesOfColumnBreaks) {
		List<Score> scores = new ArrayList<>();
		List<Integer> matches = new ArrayList<>();
		int offset = 0;
		
		for (Integer endOfColumnIndex : indexesOfColumnBreaks) {
			for (int index = offset; index <= endOfColumnIndex.intValue(); index++) {
				if (allColumnScore.matches.size() > 0 && index == allColumnScore.matches.get(0)) {
					allColumnScore.matches.remove(0);
					matches.add(index - offset);
				}
			}
			scores.add(new Score(allColumnScore.rank, matches));
			matches = new ArrayList<>();
			offset = endOfColumnIndex + 1;
		}		
		return scores;
	}
}
