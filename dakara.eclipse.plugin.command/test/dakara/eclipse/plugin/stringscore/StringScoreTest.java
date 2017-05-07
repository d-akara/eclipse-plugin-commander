package dakara.eclipse.plugin.stringscore;

import org.junit.Assert;
import org.junit.Test;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public class StringScoreTest {
	@Test
	public void verifyEmptyMatch() {
		Score score = StringScore.contains("", "abc");
		Assert.assertEquals(-1, score.rank);
	}
}
