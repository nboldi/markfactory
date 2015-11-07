package hu.elte.markfactory.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import hu.elte.markfactory.standalone.StandaloneParser;
import org.junit.Test;

public class Tests {
	
	

	private static final String TESTS_PREFIX = "tests\\";
	private static final String EXPECTED_PREFIX = "expected\\";


	@Test
	public void testEmptyClass_isIgnored() throws Exception {
		runTest(EXPECTED_PREFIX + "emptyClass", TESTS_PREFIX + "emptyClass");
	}
	
	@Test
	public void testConstruction() throws Exception {
		runTest(EXPECTED_PREFIX + "construction", TESTS_PREFIX + "construction");
	}
	
	@Test
	public void testStaticMethodCall() throws Exception {
		runTest(EXPECTED_PREFIX + "staticMethodCall", TESTS_PREFIX + "staticMethodCall");
	}
	
	@Test
	public void testMethodCall() throws Exception {
		runTest(EXPECTED_PREFIX + "methodCall", TESTS_PREFIX + "methodCall");
	}
	
	@Test
	public void testAttributeGet() throws Exception {
		runTest(EXPECTED_PREFIX + "attributeGet", TESTS_PREFIX + "attributeGet");
	}
	
	@Test
	public void testAttributeSet() throws Exception {
		runTest(EXPECTED_PREFIX + "attributeSet", TESTS_PREFIX + "attributeSet");
	}
	
	@Test
	public void testStaticAttribute() throws Exception {
		runTest(EXPECTED_PREFIX + "staticAttribute", TESTS_PREFIX + "staticAttribute");
	}
	
	@Test
	public void testStaticAttributeSet() throws Exception {
		runTest(EXPECTED_PREFIX + "staticAttributeSet", TESTS_PREFIX + "staticAttributeSet");
	}
	
	@Test
	public void testAttributeDeclaration() throws Exception {
		runTest(EXPECTED_PREFIX + "attributeDeclaration", TESTS_PREFIX + "attributeDeclaration");
	}
	
	@Test
	public void testVariableDeclaration() throws Exception {
		runTest(EXPECTED_PREFIX + "variableDeclaration", TESTS_PREFIX + "variableDeclaration");
	}
	
	@Test
	public void testCompoundOps() throws Exception {
		runTest(EXPECTED_PREFIX + "compoundOps", TESTS_PREFIX + "compoundOps");
	}
	
	
	private void runTest(String expectedDir, String testDir) {

		File[] testFiles = new File(testDir).listFiles();
		File expectedFolder = new File(expectedDir);
		Map<String, String> testFileMapping = new HashMap<>();

		for (File file : testFiles) {
			String expectedFile = expectedFolder.getAbsolutePath()
					+ File.separator + file.getName();
			testFileMapping.put(expectedFile, file.toString());
		}

		TestASTRequestor testASTRequestor = new TestASTRequestor(
				testFileMapping);

		String[] ownClassPath = System.getProperty("java.class.path").split(
				File.pathSeparator);

		StandaloneParser.loadFiles(testFileMapping.values(), testASTRequestor,
				ownClassPath);
		StandaloneParser.loadFiles(testFileMapping.keySet(), testASTRequestor,
				ownClassPath);
	}

}
