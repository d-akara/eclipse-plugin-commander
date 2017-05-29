package dakara.eclipse.plugin;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import dakara.eclipse.plugin.baseconverter.BaseAlpha26ConverterTest;
import dakara.eclipse.plugin.kavi.picklist.InputCommandTest;
import dakara.eclipse.plugin.kavi.picklist.ListRankAndSelectorTest;
import dakara.eclipse.plugin.stringscore.StringCursorTest;
import dakara.eclipse.plugin.stringscore.StringScoreTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	StringCursorTest.class,
	StringScoreTest.class,
	ListRankAndSelectorTest.class,
	InputCommandTest.class,
	BaseAlpha26ConverterTest.class
})

public class TestSuite {}
