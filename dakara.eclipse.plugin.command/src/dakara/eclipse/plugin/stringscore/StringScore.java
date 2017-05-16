package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringScore {
	private static final Score EMPTY_SCORE = new Score(0, Collections.emptyList());
	private static final Score NOT_FOUND_SCORE = new Score(-1, Collections.emptyList());
	
	public static Score containsAnyOrderWords(String match, String target) {
		int totalRank = 0;
		List<Integer> matches = new ArrayList<>();
		for (String word : splitWords(match)) {
			Score score = contains(word, target);
			if ( score.rank <= 0) return score;  // all words must be found
			totalRank += score.rank;
			matches.addAll(score.matches);
		}
		return new Score(totalRank, matches);
	}
	
	public static Score contains(String match, String target) {
		if ((match == null) || (match.length() == 0)) return EMPTY_SCORE;
		match = match.toLowerCase();
		target = target.toLowerCase();
		int index = target.indexOf(match);
		if ( index > -1 ) {
			return new Score(100 - index, fillList(index, match.length()));
		}
		return NOT_FOUND_SCORE;
	}
	
	private static String[] splitWords(String text) {
		String[] words = text.split(" ");
		return words;
	}
	
	private static List<Integer> fillList(int startNumber, int length) {
		if (length == 0) return Collections.emptyList();
		return IntStream.range(startNumber, startNumber + length).boxed().collect(Collectors.toList());
	}
	
	public static class Score {
		public final int rank;
		public final List<Integer> matches;
		public Score(int rank, List<Integer> matches) {
			this.rank = rank;
			this.matches = matches;
		}
	}
}