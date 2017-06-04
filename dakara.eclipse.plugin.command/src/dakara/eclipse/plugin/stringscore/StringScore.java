package dakara.eclipse.plugin.stringscore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
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
 *  TODO - remaining match strategies to implement
 *         1) camel case matching.  will be more useful when used for filename matching, not so much in commands
 *         2) sequence matching. there can be any distance between all letters, but must be in same order.
 */

public class StringScore {
	private static final Score EMPTY_SCORE = new Score(0, Collections.emptyList());
	private static final Score NOT_FOUND_SCORE = new Score(-1, Collections.emptyList());
	
	private BiFunction<String, StringCursor, Integer> contiguousSequenceRankingProvider;
	private Function<StringCursor, Integer> acronymRankingProvider;
	private Function<StringCursor, Integer> nonContiguousSequenceRankingProvider;
	
	// TODO consider the scoring strategies could be converted to streams and made parallel
	public StringScore(BiFunction<String, StringCursor, Integer> contiguousSequenceRankingProvider, Function<StringCursor, Integer> acronymRankingProvider, Function<StringCursor, Integer> nonContiguousSequenceRankingProvider) {
		this.contiguousSequenceRankingProvider = contiguousSequenceRankingProvider;
		this.acronymRankingProvider = acronymRankingProvider;
		this.nonContiguousSequenceRankingProvider = nonContiguousSequenceRankingProvider;
	}
	
	public Score scoreCombination(String match, String target) {
		if (match.length() == 0) return NOT_FOUND_SCORE;
		
		final String[] words = splitWords(match);
		Score score;
		if (words.length == 1) {
			score = scoreAsContiguousSequence(match, target);
			if (score.rank == 4) return score;  // perfect whole word match
			
			Score acronymScore = scoreAsAcronym(match.toLowerCase(), target.toLowerCase());
			if (acronymScore.rank > score.rank) {
				score = acronymScore;
			} 
			
			if (match.length() > 2) {
				Score nonContiguousScore = scoreAsNonContiguousSequence(match, target);
				if (nonContiguousScore.rank > score.rank) {
					score = nonContiguousScore;
				}
			}
		} else {
			score = scoreMultipleContiguousSequencesAnyOrder(words, target);			
		}
		return score;
	}
	
	public Score scoreMultipleContiguousSequencesAnyOrder(final String[] words, final String target) {
		int totalRank = 0;
		List<Integer> matches = new ArrayList<>();
		for (String word : words) {
			Score score = scoreAsContiguousSequence(word, maskRegions(target, matches));
			if ( score.rank <= 0) {
				totalRank = 0;
				break;  // all words must be found
			}
			totalRank += score.rank;
			matches.addAll(score.matches);
		}
		
		matches.sort(Comparator.naturalOrder());
		return new Score(totalRank, matches);
	}
	
	public Score scoreAsContiguousSequence(String match, String target) {
		if ((match == null) || (match.length() == 0)) return EMPTY_SCORE;
		
		match = match.toLowerCase();
		target = target.toLowerCase();
		StringCursor targetCursor = new StringCursor(target);
		
		int rank = 0;
		if (!targetCursor.moveCursorIndexOf(match).cursorPositionTerminal()) {
			rank = contiguousSequenceRankingProvider.apply(match, targetCursor);
		}
		
		if (rank > 0)
			return new Score(rank, targetCursor.markFillRangeForward(match.length()).markers());
		return NOT_FOUND_SCORE;
	}		
	
	public Score scoreAsAcronym(String searchInput, String text) {
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
			int rank = acronymRankingProvider.apply(matchesCursor);
			return new Score(rank, matchesCursor.markers());
		}
		
		return EMPTY_SCORE;
	}
	
	private StringCursor addMarkersForAcronym(StringCursor text) {
		while (!text.cursorPositionTerminal()) {
			if (Character.isAlphabetic(text.currentChar()) && !Character.isAlphabetic(text.peekPreviousChar())) {
				text.addMark(text.indexOfCursor());
			}
			text.moveCursorForward();
		}
		return text;
	}
	
	public Score scoreAsNonContiguousSequence(String match, String target) {
		if ((match == null) || (match.length() < 2)) return EMPTY_SCORE;
		
		match = match.toLowerCase();
		target = target.toLowerCase();
		StringCursor targetCursor = new StringCursor(target);
		StringCursor matchCursor = new StringCursor(match);
		matchCursor.addMark(0);
		while (!matchCursor.cursorPositionTerminal()) {
			if (!longestMatchingSequence(matchCursor, targetCursor)) break;
			
			if (!targetCursor.cursorAtWordStart() && matchCursor.indexOfCursor() - matchCursor.indexOfCurrentMark() == 1) {
				break; // non contiguous match not at word start.  bail out
			}
			
			targetCursor.markFillAlphaRangeForward(matchCursor.indexOfCursor() - matchCursor.indexOfCurrentMark());
			targetCursor.maskRegions(targetCursor.markers());
			if (!matchCursor.cursorPositionTerminal()) {
				matchCursor.addMark(matchCursor.indexOfCursor());
				matchCursor.setNextMarkCurrent();
			}
		}
		
		if (targetCursor.markers().size() == match.length()) {
			return new Score(nonContiguousSequenceRankingProvider.apply(targetCursor), 	targetCursor.markers());
		} else {
			return NOT_FOUND_SCORE;
		}
	}
	
	private boolean longestMatchingSequence(StringCursor matchCursor, StringCursor target) {
		boolean partialMatchExists = false;
		int lastFoundIndex = -1;
		while(!matchCursor.cursorPositionTerminal() && !target.cursorPositionTerminal()) {
			target.moveCursorForwardIndexOfAlphaSequenceWrapAround(matchCursor.text.substring(matchCursor.indexOfCurrentMark(), matchCursor.indexOfCursor() + 1));  // search for alpha sequence

			if (!target.cursorPositionTerminal()) matchCursor.moveCursorForward();  // match was found in target, keep advancing match
			else break; // no match found
			lastFoundIndex = target.indexOfCursor();  // track last found so target cursor can be set to starting index of sequence
			partialMatchExists = true;
		}
		target.setCursorPosition(lastFoundIndex);
		return partialMatchExists;
	}
	
	/*
	 * replace matched regions with space so we don't match them again
	 */
	private String maskRegions(String text, List<Integer> maskIndexes) {
		if (maskIndexes.size() == 0) return text;
		StringBuilder builder = new StringBuilder(text);
		maskIndexes.stream().forEach(index -> builder.setCharAt(index, ' '));
		return builder.toString();
	}
	
	private String[] splitWords(String text) {
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