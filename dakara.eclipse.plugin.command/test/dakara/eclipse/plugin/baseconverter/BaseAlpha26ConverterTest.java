package dakara.eclipse.plugin.baseconverter;

import org.junit.Assert;
import org.junit.Test;

public class BaseAlpha26ConverterTest {
	@Test
	public void verifyToAlphaConverter() {
		Base26AlphaBijectiveConverter converter = new Base26AlphaBijectiveConverter();
		Assert.assertEquals("a", converter.toAlpha(1));
		Assert.assertEquals("z", converter.toAlpha(26));
		Assert.assertEquals("aa", converter.toAlpha(27));
	}
	
	@Test
	public void verifyToNumericConverter() {
		Base26AlphaBijectiveConverter converter = new Base26AlphaBijectiveConverter();
		Assert.assertEquals(1, converter.toNumeric("a"));
		Assert.assertEquals(26, converter.toNumeric("z"));
		Assert.assertEquals(27, converter.toNumeric("aa"));
		Assert.assertEquals(703, converter.toNumeric("aaa"));
	}
	
	@Test
	public void verifyStartingRangeValue() {
		Base26AlphaBijectiveConverter converter = new Base26AlphaBijectiveConverter(26);
		Assert.assertEquals("a", converter.toAlpha(1));
		Assert.assertEquals("z", converter.toAlpha(26));
		Assert.assertEquals("aa", converter.toAlpha(27));
		Assert.assertEquals("ab", converter.toAlpha(28));
		Assert.assertEquals("all", converter.toAlpha(1000));
	}	
	
	@Test
	public void verifyStartingRangeValue2() {
		Base26AlphaBijectiveConverter converter = new Base26AlphaBijectiveConverter(1000);
		Assert.assertEquals("aaa", converter.toAlpha(1));
		Assert.assertEquals("aaz", converter.toAlpha(26));
		Assert.assertEquals("aba", converter.toAlpha(27));
		Assert.assertEquals("abb", converter.toAlpha(28));
		Assert.assertEquals("bml", converter.toAlpha(1000));
	}	
	
	@Test
	public void verifyStartingRangeValue3() {
		Base26AlphaBijectiveConverter converter = new Base26AlphaBijectiveConverter(1000);
		Assert.assertEquals(1, converter.toNumeric("aaa"));
		Assert.assertEquals(26, converter.toNumeric("aaz"));
		Assert.assertEquals(27, converter.toNumeric("aba"));
		Assert.assertEquals(28, converter.toNumeric("abb"));
		Assert.assertEquals(1000, converter.toNumeric("bml"));
	}	
}
