package com.dbdeploy;

import com.dbdeploy.appliers.DirectToDbApplierTest;
import com.dbdeploy.appliers.TemplateBasedApplierTest;
import com.dbdeploy.database.QueryStatementSplitterOracleTest;
import com.dbdeploy.database.QueryStatementSplitterTest;
import com.dbdeploy.database.ScriptGenerationTest;
import com.dbdeploy.integration.DirectToDbIntegrationTest;
import com.dbdeploy.integration.OutputToFileIntegrationTest;
import com.dbdeploy.scripts.ChangeScriptCreatorTest;
import com.dbdeploy.scripts.ChangeScriptRepositoryTest;
import com.dbdeploy.scripts.ChangeScriptTest;
import com.dbdeploy.scripts.FilenameParserTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(value = Suite.class)
@SuiteClasses(value = { PrettyPrinterTest.class, ControllerTest.class, DbDeployTest.class, ChangeScriptTest.class,
		ChangeScriptCreatorTest.class, ChangeScriptRepositoryTest.class, FilenameParserTest.class,
		DirectToDbIntegrationTest.class, OutputToFileIntegrationTest.class, ScriptGenerationTest.class,
		QueryStatementSplitterTest.class, QueryStatementSplitterOracleTest.class, DirectToDbApplierTest.class,
		TemplateBasedApplierTest.class })
public class TestSuite {

}
