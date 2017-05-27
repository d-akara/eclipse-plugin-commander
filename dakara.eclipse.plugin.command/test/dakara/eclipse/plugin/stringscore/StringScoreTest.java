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
}
