package hu.elte.markfactory.standalone;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;

public class StandaloneConverter {

	List<String> inputFiles = new LinkedList<>();
	private ASTParser parser;

	public static void main(String[] args) throws IOException {
		StandaloneConverter converter = new StandaloneConverter();
		converter.parseCmdArgs(args);
		converter.convert();
	}

	public StandaloneConverter() {
		parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
	}

	private void parseCmdArgs(String[] args) {

	}

	private void convert() {
		String[] encodings = new String[inputFiles.size()];
		String[] sourceFiles = new String[inputFiles.size() - 1];
		for (int i = 1; i < inputFiles.size(); i++) {
			encodings[i - 1] = "UTF8";
			sourceFiles[i - 1] = inputFiles.get(i);
		}
		parser.setEnvironment(new String[] { "." + File.pathSeparator + inputFiles.get(0) },
				new String[] { "." + File.pathSeparator + inputFiles.get(0) }, null, true);
		// parser.createASTs(sourceFiles, encodings, new String[] {},
		// new RewriterASTRequestor(), null);
	}

}
