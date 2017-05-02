package dakara.eclipse.plugin.command.handlers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringScore {
	public static Score contains(String match, String target) {
		match = match.toLowerCase();
		target = target.toLowerCase();
		int index = target.indexOf(match);
		if ( index > -1 ) {
			return new Score(100 - index, fillList(index, match.length()));
		}
		return new Score(-1, Collections.emptyList() );
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