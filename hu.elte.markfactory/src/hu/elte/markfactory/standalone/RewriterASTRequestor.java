package hu.elte.markfactory.standalone;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jface.text.Document;
import hu.elte.markfactory.rewrite.AutocheckVisitor;
import org.eclipse.text.edits.TextEdit;

public class RewriterASTRequestor extends FileASTRequestor {

	private Map<String, String> inputOutputFiles;

	public RewriterASTRequestor(Map<String, String> inputOutputFiles) {
		this.inputOutputFiles = inputOutputFiles;
	}

	@Override
	public void acceptAST(String sourceFilePath, CompilationUnit astRoot) {
		try {
			if (!inputOutputFiles.containsKey(sourceFilePath)) {
				throw new RuntimeException(
						"Input to output files map does not contain output file name for "
								+ sourceFilePath);
			}

			AST ast = astRoot.getAST();

			astRoot.recordModifications();

			Document document = new Document(loadFile(new File(sourceFilePath)));

			AutocheckVisitor visitor = new AutocheckVisitor(ast);
			astRoot.accept(visitor);
			TextEdit edits = astRoot.rewrite(document, null);
			edits.apply(document);
			String newSource = document.get();

			writeOutResult(inputOutputFiles.get(sourceFilePath), newSource);

		} catch (Exception e) {
			System.err.println("Error during the transformation");
			e.printStackTrace();
		}
	}

	private void writeOutResult(String outputPath, String newSource)
			throws IOException {
		File outFile = new File(outputPath);
		if (!outFile.exists()) {
			outFile.getParentFile().mkdirs();
			outFile.createNewFile();
		}
		PrintWriter out = new PrintWriter(outFile);
		out.println(newSource);
		out.close();
	}

	private String loadFile(File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append('\n');
		}
		reader.close();
		return sb.toString();
	}

}
