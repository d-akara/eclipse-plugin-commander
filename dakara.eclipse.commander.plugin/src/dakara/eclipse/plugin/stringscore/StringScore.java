package dakara.eclipse.plugin.stringscore;

import java.util.Comparator;
import java.util.List;
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
 *         1) sequence matching. there can be any distance between all letters, but must be in same order. 
 *            - possibly start filter with special char '#'
 *         2) super fuzzy.  all items which contain letters in any order.  special char input '?'
 *         3) Negative matching.  Prefix words with ! to match when word is not found
 */

public class StringScore {
	private static final Score EMPTY_SCORE = new Score(0, new IntArrayList(0));
	private static final Score NOT_FOUND_SCORE = new Score(-1, new IntArrayList(0));
	private static final Score INVERSE_FOUND_SCORE = new Score(1, new IntArrayList(0));	
	
	private BiFunction<String, StringCursor, Integer> contiguousSequenceRankingProvider;
	private Function<StringCursor, Integer> acronymRankingProvider;
	private Function<StringCursor, Integer> nonContiguousSequenceRankingProvider;
	
	public StringScore(BiFunction<String, StringCursor, Integer> contiguousSequenceRankingProvider, Function<StringCursor, Integer> acronymRankingProvider, Function<StringCursor, Integer> nonContiguousSequenceRankingProvider) {
		this.contiguousSequenceRankingProvider = contiguousSequenceRankingProvider;
		this.acronymRankingProvider = acronymRankingProvider;
		this.nonContiguousSequenceRankingProvider = nonContiguousSequenceRankingProvider;
	}
	
	public Score parseMatchAndScore(String match, String target) {
		return parseMatchAndScore(new ScoreFilterOptions(match), target);
	}
	
	public Score parseMatchAndScore(final ScoreFilterOptions filterOptions, String target) {
		if ((target == null) || (target.length() == 0)) return NOT_FOUND_SCORE;
		final StringCursorPrimitive targetCursorPrimitive = new StringCursorPrimitive(target.trim());
		return parseMatchAndScore(filterOptions, targetCursorPrimitive);
	}
	
	// TODO - need to accept a StringCursorPrimitive for target
	// the initialization costs shows up in profiling
	public Score parseMatchAndScore(final ScoreFilterOptions filterOptions, final StringCursorPrimitive targetCursorPrimitive) {
		final StringCursorPrimitive match = filterOptions.filterTextCursorPrimitive;
		
		if (filterOptions.inverseMatch) { 
			if (containsString(targetCursorPrimitive, filterOptions.inverseFilters)) return EMPTY_SCORE;
			else if (match.length() == 0) return INVERSE_FOUND_SCORE; // no filter supplied, only negative filter
		}
		if ((match.length() == 0)) return NOT_FOUND_SCORE;
		
		final String[] words = splitWords(match.asString());
		
		Score score = determineScore(filterOptions.scoreAsAcronym, filterOptions.scoreAsLiteral, match, targetCursorPrimitive, words);
		return score;
	}
	
	private boolean containsString(StringCursorPrimitive target, List<String> filters) {
		for (String filter : filters) {
			if (target.indexOf(filter) != -1) return true;
		}
		return false;
	}
	
	private Score determineScore(boolean scoreAsAcronym, boolean scoreAsLiteral, StringCursorPrimitive matchCursorPrimitive, StringCursorPrimitive targetCursorPrimitive, final String[] words) {
		Score score;
		if (scoreAsAcronym) {
			// If there is a leading space, then treat all chars as acronym
			score = scoreAsAcronym(matchCursorPrimitive, targetCursorPrimitive);
		} else if (scoreAsLiteral) {
			// If there is a trailing space, then treat all chars following as literal
			score = scoreAsContiguousSequence(matchCursorPrimitive, targetCursorPrimitive);
		} else if (words.length == 1) {
			score = scoreAsContiguousSequence(matchCursorPrimitive, targetCursorPrimitive);
			if (score.rank == 4) return score;  // perfect whole word match
			
			Score acronymScore = scoreAsAcronym(matchCursorPrimitive, targetCursorPrimitive);
			if (acronymScore.rank == 4) return acronymScore; // perfect acronym match;
			
			if (acronymScore.rank > score.rank) {
				score = acronymScore;
			} 
			
			if (matchCursorPrimitive.length() > 2) {
				Score nonContiguousScore = scoreAsNonContiguousSequence(matchCursorPrimitive, targetCursorPrimitive);
				if (nonContiguousScore.rank > score.rank) {
					score = nonContiguousScore;
				}
			}
		} else {
			score = scoreMultipleContiguousSequencesAnyOrder(words, targetCursorPrimitive);			
		}
		return score;
	}
	
	public Score scoreMultipleContiguousSequencesAnyOrder(final String[] words, final StringCursorPrimitive target) {
		int totalRank = 0;
		IntArrayList matches = new IntArrayList();
		for (String word : words) {
			StringCursor targetCursor = new StringCursor(target).maskRegions(matches);
			Score score = scoreAsContiguousSequence(new StringCursorPrimitive(word), targetCursor.getCursorPrimitive());
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
		return scoreAsContiguousSequence(new StringCursorPrimitive(match), new StringCursorPrimitive(target));
	}
	public Score scoreAsContiguousSequence(StringCursorPrimitive match, StringCursorPrimitive target) {
		if ((match == null) || (match.length() == 0)) return EMPTY_SCORE;
		
		StringCursor targetCursor = new StringCursor(target);
		
		int rank = 0;
		while (!targetCursor.moveCursorForwardIndexOf(match.asString()).cursorPositionTerminal()) {
			rank = contiguousSequenceRankingProvider.apply(match.asString(), targetCursor);
			if (rank > 0) break;
			targetCursor.moveCursorForward();
		}
		
		if (rank > 0)
			return new Score(rank, targetCursor.markFillRangeForward(match.length()).markers());
		return EMPTY_SCORE;
	}
	
	public Score scoreAsAcronym(String match, String target) {
		return scoreAsAcronym(new StringCursorPrimitive(match), new StringCursorPrimitive(target));
	}
	public Score scoreAsAcronym(StringCursorPrimitive searchInput, StringCursorPrimitive text) {
		StringCursor matchesCursor = new StringCursor(text);
		StringCursor inputCursor = new StringCursor(searchInput);
		
		while (!matchesCursor.cursorPositionTerminal() && !inputCursor.cursorPositionTerminal()) {
			matchesCursor.moveCursorForwardPartialWordStart();
			if (matchesCursor.cursorPositionTerminal()) break;
			if (matchesCursor.currentChar() == inputCursor.currentChar()) {
				inputCursor.moveCursorForward();
				matchesCursor.addMark(matchesCursor.indexOfCursor());
			}
			matchesCursor.moveCursorForward();
		}
		
		// did we complete all matches from the input
		if (inputCursor.cursorPositionTerminal()) {
			int rank = acronymRankingProvider.apply(matchesCursor);
			return new Score(rank, matchesCursor.markers());
		}
		
		return EMPTY_SCORE;
	}
	
	
	public Score scoreAsNonContiguousSequence(String match, String target) {
		return scoreAsNonContiguousSequence(new StringCursorPrimitive(match), new StringCursorPrimitive(target));
	}
	
	public Score scoreAsNonContiguousSequence(StringCursorPrimitive match, StringCursorPrimitive target) {
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
			return EMPTY_SCORE;
		}
	}

	private boolean veryWeakMatch(StringCursor targetCursor, StringCursor matchCursor) {
		// If we are not at the start and selected count less than 2, this is too weak.
		if (!targetCursor.cursorAtPartialWordStart() && matchCursor.indexOfCursor() - matchCursor.indexOfCurrentMark() < 3) return true;
		// If we are starting at the end of a word, this is a weak match
		if (targetCursor.cursorAtPartialWordEnd() && !targetCursor.cursorAtPartialWordStart()) return true;
		
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
			target.moveCursorForwardIndexOf(remainingPartToMatch);
			
			if (!target.cursorPositionTerminal()) matchCursor.moveCursorForward();  // match was found in target, keep advancing match
			else break; // no match found
			lastFoundIndex = target.indexOfCursor();  // track last found so target cursor can be set to starting index of sequence
			partialMatchExists = true;
		}
		target.setCursorPosition(lastFoundIndex);
		return partialMatchExists;
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