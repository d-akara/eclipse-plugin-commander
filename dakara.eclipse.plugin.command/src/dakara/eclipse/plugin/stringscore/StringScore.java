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
 * - by importance:
 * 	- exact match
 *  - multiple word full match
 *  - single word full match
 *  - acronym match
 *  
 * - bonuses:
 *  - word or acronym starts at first char
 *  - word or acronym contiguous
 * 
 * - minuses:
 *  - gaps between words or acronym
 *  - matches in middle of word
 *  
 *  TODO - Need a new way to score across fields or columns vs doing each field individually as we do now
 *         Multiple words should match across fields as if the fields were concatenated as a single field
 *         All words must be found, but can exist across fields
 *         This will require some redesign, but will be very powerful
 *  
 *  TODO - remaining match strategies to implement
 *         1) camel case matching.  will be more useful when used for filename matching, not so much in commands
 *         2) sequence matching. there can be any distance between all letters, but must be in same order.
 */

public class StringScore {
	private static final Score EMPTY_SCORE = new Score(0, Collections.emptyList());
	private static final Score NOT_FOUND_SCORE = new Score(-1, Collections.emptyList());
	
	public static Score scoreCombination(String match, String target) {
		final String[] words = splitWords(match);
		Score wordsScore = scoreMultipleContainsAnyOrder(words, target);
		if (words.length == 1) {
			Score acronymScore = scoreAsAcronym(match.toLowerCase(), target.toLowerCase());
			if (acronymScore.rank > wordsScore.rank) {
				return acronymScore;
			} 
		}
		return wordsScore;
	}
	
	public static Score scoreMultipleContainsAnyOrder(final String[] words, final String target) {
		int totalRank = 0;
		List<Integer> matches = new ArrayList<>();
		for (String word : words) {
			Score score = scoreAsContains(word, maskRegions(target, matches));
			if ( score.rank <= 0) {
				totalRank = 0;
				break;  // all words must be found
			}
			totalRank += score.rank;
			matches.addAll(score.matches);
		}
		return new Score(totalRank, matches);
	}
	
	public static Score scoreAsContains(String match, String target) {
		if ((match == null) || (match.length() == 0)) return EMPTY_SCORE;
		
		match = match.toLowerCase();
		target = target.toLowerCase();
		StringCursor targetCursor = new StringCursor(target);
		
		int rank = 0;
		if (!targetCursor.moveCursorIndexOf(match).cursorPositionTerminal()) {
			rank = rankContainsMatches(match, targetCursor);
		}
		
		if (rank > 0)
			return new Score(rank, targetCursor.markFillRangeForward(match.length()).markers());
		return NOT_FOUND_SCORE;
	}

	private static int rankContainsMatches(String match, StringCursor targetCursor) {
		int rank = 0;
		final boolean fullMatch = targetCursor.wordAtCursor().equals(match);  // did we match full word
		if ( fullMatch ) {
			rank = 3;
		} else {
			if (targetCursor.cursorAtWordStart())
				rank = 2;
			else if (match.length() > 2)  // 1 or 2 character matches are very weak when not at beginning of word, lets not show them at all.
				rank = 1;
		}
		
		// bonuses
		if (targetCursor.indexOfCursor() == 0) {
			// Our match is at the very beginning
			rank += 1;
		}
		return rank;
	}		
	
	public static Score scoreAsAcronym(String searchInput, String text) {
		StringCursor matchesCursor = new StringCursor(text);
		StringCursor acronymCursor = addMarkersForAcronym(new StringCursor(text));
		StringCursor inputCursor = new StringCursor(searchInput);
		
		while (!acronymCursor.markerPositionTerminal() && !inputCursor.cursorPositionTerminal()) {
			if (acronymCursor.currentMarker() == inputCursor.currentChar()) {
				inputCursor.moveCursorForward();
				matchesCursor.addMark(acronymCursor.indexOfCurrentMark());
			}
			acronymCursor.setNextMarkCurrent();
		}
		
		// did we complete all matches from the input
		if (inputCursor.cursorPositionTerminal()) {
			int rank = rankAcronymMatches(matchesCursor);
			return new Score(rank, matchesCursor.markers());
		}
		
		return EMPTY_SCORE;
	}

	private static int rankAcronymMatches(StringCursor matchesCursor) {
		int rank = 3;
		// apply bonus for first character match
		if (matchesCursor.setFirstMarkCurrent().indexOfCurrentMark() == 0) 
			rank += 1;
		// subtract for weak matches.  If there are more than 1 gap reduce ranking
		// if there are a large number of gaps, remove entirely
		int countUnMarkedWords = matchesCursor.countUnMarkedWordsBetweenMarkers(0, matchesCursor.markers().size() - 1);
		if (countUnMarkedWords > 0) {
			rank -=1;
		}
		if (countUnMarkedWords > 2) {
			rank = 0;
		}
		return rank;
	}
	
	private static StringCursor addMarkersForAcronym(StringCursor text) {
		while (!text.cursorPositionTerminal()) {
			if (Character.isAlphabetic(text.currentChar()) && !Character.isAlphabetic(text.peekPreviousChar())) {
				text.addMark(text.indexOfCursor());
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