package dakara.eclipse.plugin.stringscore;

import org.junit.Assert;
import org.junit.Test;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class StringScoreTest {
	@Test
	public void verifyEmptyMatch() {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		
		Score score = stringScore.scoreAsContiguousSequence("", "abc");
		Assert.assertEquals(0, score.rank);
	}
	
	@Test
	public void verifyContainsScoring() {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		
		Score score = stringScore.scoreAsContiguousSequence("abc", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = stringScore.scoreAsContiguousSequence("def", "abc def ghi jkl");
		Assert.assertEquals(3, score.rank);
		
		score = stringScore.scoreAsContiguousSequence("e", "abc def ghi jkl");
		Assert.assertEquals(-1, score.rank);
		
		score = stringScore.scoreAsContiguousSequence("ef", "abc def ghi jklmn");
		Assert.assertEquals(-1, score.rank);
		
		score = stringScore.scoreAsContiguousSequence("de", "abc def ghi jklmn");
		Assert.assertEquals(2, score.rank);
		
		score = stringScore.scoreAsContiguousSequence("klm", "abc def ghi jklmn");
		Assert.assertEquals(1, score.rank);
	}
	
	@Test
	public void verifyAcronymScoring() {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		
		Score score = stringScore.scoreAsAcronym("abc", "abc def ghi jkl");
		Assert.assertEquals(0, score.rank);
		
		score = stringScore.scoreAsAcronym("ad", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = stringScore.scoreAsAcronym("aj", "abc def ghi jkl");
		Assert.assertEquals(3, score.rank);
		
		score = stringScore.scoreAsAcronym("adz", "abc def ghi jklmn");
		Assert.assertEquals(0, score.rank);
		
		score = stringScore.scoreAsAcronym("dgj", "abc def ghi jklmn");
		Assert.assertEquals(3, score.rank);
		
		score = stringScore.scoreAsAcronym("dj", "abc def ghi jklmn");
		Assert.assertEquals(2, score.rank);
		
		score = stringScore.scoreAsAcronym("ax", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(0, score.rank);
	}
	
	@Test
	public void verifyCombinationScoring() {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		
		Score score = stringScore.scoreCombination("abc", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = stringScore.scoreCombination("ad", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = stringScore.scoreCombination("aj", "abc def ghi jkl");
		Assert.assertEquals(3, score.rank);
		
		score = stringScore.scoreCombination("adz", "abc def ghi jklmn");
		Assert.assertEquals(0, score.rank);
		
		score = stringScore.scoreCombination("dgj", "abc def ghi jklmn");
		Assert.assertEquals(3, score.rank);
		
		score = stringScore.scoreCombination("dj", "abc def ghi jklmn");
		Assert.assertEquals(2, score.rank);
		
		score = stringScore.scoreCombination("ax", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(0, score.rank);
		
	}
	
	@Test
	public void emptyScoring() {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		// TODO investigate inconsistencies between scores of 0 and -1
		Score score = stringScore.scoreCombination("", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(-1, score.rank);	
		
		score = stringScore.scoreAsContiguousSequence("", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(0, score.rank);
	}
	
	@Test
	public void multiWordOutOfOrderScoring() {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		Score score = stringScore.scoreCombination("def abc", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(6, score.matches.size());	
		Assert.assertEquals(7, score.rank);	
		

	}
	
	@Test
	public void nonContiguousSequenceScoring() {
		StringScore stringScore = new StringScore(StringScoreRanking.standardContiguousSequenceRanking(), StringScoreRanking.standardAcronymRanking(), StringScoreRanking.standardNonContiguousSequenceRanking());
		Score score = stringScore.scoreAsNonContiguousSequence("defghi", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(6, score.matches.size());	
		Assert.assertEquals(3, score.rank);	
		Assert.assertEquals("defghi", new StringCursor("abc def ghi jklmn mop xyz").setMarkers(score.matches).markersAsString());
		
		score = stringScore.scoreAsNonContiguousSequence("ghidef", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(6, score.matches.size());	
		Assert.assertEquals(3, score.rank);			
		
		score = stringScore.scoreAsNonContiguousSequence("ghidef", "abc def jklmn mop xyz ghi");
		Assert.assertEquals(6, score.matches.size());	
		Assert.assertEquals(2, score.rank);			
		
		score = stringScore.scoreAsNonContiguousSequence("defghi", "abc defg jklmn mop xyz ghi");
		Assert.assertEquals(6, score.matches.size());	
		Assert.assertEquals(2, score.rank);			
		
		score = stringScore.scoreAsNonContiguousSequence("defghi", "abcd vbe deb def xyz ghi");
		Assert.assertEquals(6, score.matches.size());	
		Assert.assertEquals(2, score.rank);			
	}
	
}
