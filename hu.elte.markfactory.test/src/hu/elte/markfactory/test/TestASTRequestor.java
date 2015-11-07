package hu.elte.markfactory.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import hu.elte.markfactory.rewrite.AutocheckVisitor;
import hu.elte.markfactory.rewrite.ModificationRecordingVisitor;

public class TestASTRequestor extends FileASTRequestor {

	private Map<String, String> testFileMapping = new HashMap<>();
	private Map<String, CompilationUnit> actualResults = new HashMap<>();

	public TestASTRequestor(Map<String, String> testFileMapping) {
		this.testFileMapping = testFileMapping;
	}

	@Override
	public void acceptAST(String sourceFilePath, CompilationUnit compUnit) {
		checkForProblems(sourceFilePath, compUnit);

		if (isTestFile(sourceFilePath)) {
			ModificationRecordingVisitor visitor = new AutocheckVisitor(
					compUnit.getAST());
			compUnit.accept(visitor);
			actualResults.put(sourceFilePath, compUnit);
		} else if (isExpectedFile(sourceFilePath)
				&& actualResults.containsKey(testFileMapping
						.get(sourceFilePath))) {
			CompilationUnit actual = actualResults.get(testFileMapping
					.get(sourceFilePath));
			boolean equals = ASTCompare.equals(compUnit, actual);
			if (!equals) {
				throw new RuntimeException(
						"The expected and the actual AST does not match. Expected file: "
								+ sourceFilePath + ", actual content:\n"
								+ actual);
			}
		} else {
			throw new RuntimeException("Unexpected AST: " + sourceFilePath);
		}
	}

	private void checkForProblems(String sourceFilePath,
			CompilationUnit compUnit) {
		StringBuilder errors = new StringBuilder();
		boolean wasProblem = false;
		for (IProblem problem : compUnit.getProblems()) {
			if (problem.isError()) {
				errors.append(problem.getOriginatingFileName());
				errors.append(" ");
				errors.append(problem.getSourceLineNumber());
				errors.append(": ");
				errors.append(problem.getMessage());
				errors.append("\n");
				wasProblem = true;
			}
		}
		if (wasProblem) {
			throw new RuntimeException("Problems while parsing "
					+ sourceFilePath + ":\n" + errors);
		}
	}

	private boolean isExpectedFile(String sourceFilePath) {
		return testFileMapping.containsKey(sourceFilePath);
	}

	private boolean isTestFile(String sourceFilePath) {
		return testFileMapping.containsValue(sourceFilePath);
	}

}
