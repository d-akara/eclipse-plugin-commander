package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * scoring strategies:
 * - rank by distance found from beginning of string
 * - rank by size of word match
 * - rank by multiple word matches
 * - rank by matching first letters of words
 * - rank by contiguous characters found
 * - single characters only match word start?
 *
 */
public class StringScore {
	private static final Score EMPTY_SCORE = new Score(0, Collections.emptyList());
	private static final Score NOT_FOUND_SCORE = new Score(-1, Collections.emptyList());
	
	// TODO run all permutations and keep highest score
	public static Score containsAnyOrderWords(String match, String target) {
		int totalRank = 0;
		List<Integer> matches = new ArrayList<>();
		String words[] = splitWords(match);
		for (String word : words) {
			Score score = contains(word, maskRegions(target, matches));
			if ( score.rank <= 0) {
				totalRank = 0;
				break;  // all words must be found
			}
			totalRank += score.rank;
			matches.addAll(score.matches);
		}
		
		if (words.length == 1) {
			Score acronymScore = scoreAsAcronym(match.toLowerCase(), target.toLowerCase());
			if (acronymScore.rank > totalRank) {
				return acronymScore;
			} 
		}
		return new Score(totalRank, matches);
	}
	
	
	
	private static Score scoreAsAcronym(String searchInput, String text) {
		StringCursor matchesCursor = new StringCursor(text);
		StringCursor acronymCursor = addMarkersForAcronym(new StringCursor(text));
		StringCursor inputCursor = new StringCursor(searchInput);
		
		while (!acronymCursor.markerPositionTerminal() && !inputCursor.cursorPositionTerminal()) {
			if (acronymCursor.currentMarker() == inputCursor.currentChar()) {
				inputCursor.moveCursorForward();
				matchesCursor.addMarker(acronymCursor.indexOfMarker());
			}
			acronymCursor.advanceMarker();
		}
		
		// did we complete all matches from the input
		if (inputCursor.cursorPositionTerminal()) {
			return new Score(4, matchesCursor.markers());
		}
		
		return EMPTY_SCORE;
	}
	
	private static StringCursor addMarkersForAcronym(StringCursor text) {
		while (!text.cursorPositionTerminal()) {
			if (Character.isAlphabetic(text.currentChar()) && !Character.isAlphabetic(text.peekPreviousChar())) {
				text.addMarker(text.indexOfCursor());
			}
			text.moveCursorForward();
		}
		return text;
	}
	
	
	/*
	 * replace matched regions with space so we don't match them again
	 */
	private static String maskRegions(String text, List<Integer> maskIndexes) {
		if (maskIndexes.size() == 0) return text;
		StringBuilder builder = new StringBuilder(text);
		maskIndexes.stream().forEach(index -> builder.setCharAt(index, ' '));
		return builder.toString();
	}
	
	// TODO don't have infinite scoring.  Need to have ranges (0-10) for each algorithm
	// score whole word matching higher vs partial includes
	public static Score contains(String match, String target) {
		if ((match == null) || (match.length() == 0)) return EMPTY_SCORE;
		
		match = match.toLowerCase();
		target = target.toLowerCase();
		StringCursor targetCursor = new StringCursor(target);
		boolean fullMatch = targetCursor.moveCursorIndexOf(match).wordAtCursor().equals(match);  // did we match full word
		if ( fullMatch ) {
			return new Score(5, targetCursor.markRangeForward(match.length()).markers());
		} else if (!targetCursor.cursorPositionTerminal()) {
			return new Score(2, targetCursor.markRangeForward(match.length()).markers());
		}
		return NOT_FOUND_SCORE;
	}
	
	private static String[] splitWords(String text) {
		String[] words = text.split(" ");
		return words;
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