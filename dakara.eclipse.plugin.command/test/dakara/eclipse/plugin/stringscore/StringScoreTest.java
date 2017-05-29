package dakara.eclipse.plugin.stringscore;

import org.junit.Assert;
import org.junit.Test;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class StringScoreTest {
	@Test
	public void verifyEmptyMatch() {
		Score score = StringScore.scoreAsContains("", "abc");
		Assert.assertEquals(0, score.rank);
	}
	
	@Test
	public void verifyContainsScoring() {
		Score score = StringScore.scoreAsContains("abc", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = StringScore.scoreAsContains("def", "abc def ghi jkl");
		Assert.assertEquals(3, score.rank);
		
		score = StringScore.scoreAsContains("e", "abc def ghi jkl");
		Assert.assertEquals(-1, score.rank);
		
		score = StringScore.scoreAsContains("ef", "abc def ghi jklmn");
		Assert.assertEquals(-1, score.rank);
		
		score = StringScore.scoreAsContains("de", "abc def ghi jklmn");
		Assert.assertEquals(2, score.rank);
		
		score = StringScore.scoreAsContains("klm", "abc def ghi jklmn");
		Assert.assertEquals(1, score.rank);
	}
	
	@Test
	public void verifyAcronymScoring() {
		Score score = StringScore.scoreAsAcronym("abc", "abc def ghi jkl");
		Assert.assertEquals(0, score.rank);
		
		score = StringScore.scoreAsAcronym("ad", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = StringScore.scoreAsAcronym("aj", "abc def ghi jkl");
		Assert.assertEquals(3, score.rank);
		
		score = StringScore.scoreAsAcronym("adz", "abc def ghi jklmn");
		Assert.assertEquals(0, score.rank);
		
		score = StringScore.scoreAsAcronym("dgj", "abc def ghi jklmn");
		Assert.assertEquals(3, score.rank);
		
		score = StringScore.scoreAsAcronym("dj", "abc def ghi jklmn");
		Assert.assertEquals(2, score.rank);
		
		score = StringScore.scoreAsAcronym("ax", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(0, score.rank);
	}
	
	@Test
	public void verifyCombinationScoring() {
		Score score = StringScore.scoreCombination("abc", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = StringScore.scoreCombination("ad", "abc def ghi jkl");
		Assert.assertEquals(4, score.rank);
		
		score = StringScore.scoreCombination("aj", "abc def ghi jkl");
		Assert.assertEquals(3, score.rank);
		
		score = StringScore.scoreCombination("adz", "abc def ghi jklmn");
		Assert.assertEquals(0, score.rank);
		
		score = StringScore.scoreCombination("dgj", "abc def ghi jklmn");
		Assert.assertEquals(3, score.rank);
		
		score = StringScore.scoreCombination("dj", "abc def ghi jklmn");
		Assert.assertEquals(2, score.rank);
		
		score = StringScore.scoreCombination("ax", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(0, score.rank);
		
	}
	
	@Test
	public void emptyScoring() {
		// TODO investigate inconsistencies between scores of 0 and -1
		Score score = StringScore.scoreCombination("", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(-1, score.rank);	
		
		score = StringScore.scoreAsContains("", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(0, score.rank);
	}
	
	@Test
	public void multiWordOutOfOrderScoring() {
		Score score = StringScore.scoreCombination("def abc", "abc def ghi jklmn mop xyz");
		Assert.assertEquals(6, score.matches.size());	
		Assert.assertEquals(7, score.rank);	
		

	}
	
}
