package dakara.eclipse.plugin.stringscore;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.IntArrayList;
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
	private static final Score EMPTY_SCORE = new Score(0, new IntArrayList(0));
	private static final Score NOT_FOUND_SCORE = new Score(-1, new IntArrayList(0));
	
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
		if ((match.length() == 0) || (target == null) || (target.length() == 0)) return NOT_FOUND_SCORE;
		match = match.toLowerCase();
		target = target.toLowerCase();
		final String[] words = splitWords(match);
		Score score;
		
		if (match.charAt(0) == ' ' || match.charAt(match.length() - 1) == ' ') {
			// If there is a leading or trailing space, then treat all chars following as literal
			score = scoreAsContiguousSequence(match.trim(), target);
		} else if (words.length == 1) {
			score = scoreAsContiguousSequence(match, target);
			if (score.rank == 4) return score;  // perfect whole word match
			
			Score acronymScore = scoreAsAcronym(match, target);
			if (acronymScore.rank == 4) return acronymScore; // perfect acronym match;
			
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
		IntArrayList matches = new IntArrayList();
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
		
		StringCursor targetCursor = new StringCursor(target);
		
		int rank = 0;
		while (!targetCursor.moveCursorForwardIndexOf(match).cursorPositionTerminal()) {
			rank = contiguousSequenceRankingProvider.apply(match, targetCursor);
			if (rank > 0) break;
			targetCursor.moveCursorForward();
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
		char previousChar = text.peekPreviousChar();
		boolean previousIsAlpha = Character.isAlphabetic(previousChar);
		while (!text.cursorPositionTerminal()) {
			char currentChar = text.currentChar();
			boolean currentIsAlpha = Character.isAlphabetic(currentChar);
			// TODO hot method
			// maybe capture current char in variable and reuse for previous char
			if (currentIsAlpha && !previousIsAlpha) {
				text.addMark(text.indexOfCursor());
			}
			text.moveCursorForward();
			previousChar = currentChar;
			previousIsAlpha = currentIsAlpha;
		}
		return text;
	}
	
	public Score scoreAsNonContiguousSequence(String match, String target) {
		if ((match == null) || (match.length() < 2)) return EMPTY_SCORE;
		
		StringCursor targetCursor = new StringCursor(target);
		StringCursor matchCursor = new StringCursor(match);
		matchCursor.addMark(0);
		outer: while (!matchCursor.cursorPositionTerminal()) {
			while (true) {
				// Attempt to find the largest match.
				// updates the cursors with location of partial match completed
				// returns false if no match possible
				if (!longestMatchingSequence(matchCursor, targetCursor))	break outer;
				if (veryWeakMatch(targetCursor, matchCursor)) {
					// try again
					// reset match position
					matchCursor.setCursorPosition(matchCursor.indexOfCurrentMark());
					// advance target by 1
					targetCursor.moveCursorForward();
					continue;
				} 
				break;
			}
			
			targetCursor.markFillRangeForward(matchCursor.indexOfCursor() - matchCursor.indexOfCurrentMark());
			targetCursor.maskRegions(targetCursor.markers());
			if (!matchCursor.cursorPositionTerminal()) {
				matchCursor.addMark(matchCursor.indexOfCursor());
				matchCursor.setNextMarkCurrent();
			}
			
			// attempt to find earliest match for all matches
			targetCursor.setCursorPosition(0);
		}
		
		if (targetCursor.markers().size() == match.length()) {
			return new Score(nonContiguousSequenceRankingProvider.apply(targetCursor), 	targetCursor.markers());
		} else {
			return NOT_FOUND_SCORE;
		}
	}

	private boolean veryWeakMatch(StringCursor targetCursor, StringCursor matchCursor) {
		// If we are not at the start and selected count less than 2, this is too weak.
		if (!targetCursor.cursorAtWordStart() && matchCursor.indexOfCursor() - matchCursor.indexOfCurrentMark() < 3) return true;
		// If we are starting at the end of a word, this is a weak match
		if (targetCursor.cursorAtWordEnd()) return true;
		
		return false;
	}
	
	/*
	 * Attempt to find the longest matching sequence from match within target.
	 * Updates both the match and target locations for match found
	 * returns false if no match could be found
	 */
	private boolean longestMatchingSequence(StringCursor matchCursor, StringCursor target) {
		boolean partialMatchExists = false;
		int lastFoundIndex = -1;
		while(!matchCursor.cursorPositionTerminal() && !target.cursorPositionTerminal()) {
			String remainingPartToMatch = matchCursor.text.substring(matchCursor.indexOfCurrentMark(), matchCursor.indexOfCursor() + 1);
			// TODO consider repeating to find better match starting at word beginning
			// Need to check bail out condition here.  If will bail out, then try again
			target.moveCursorForwardIndexOf(remainingPartToMatch);
			
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
	private String maskRegions(String text, IntArrayList maskIndexes) {
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
		public final IntArrayList matches;
		public Score(int rank, IntArrayList matches) {
			this.rank = rank;
			this.matches = matches;
		}
	}
	
	
}