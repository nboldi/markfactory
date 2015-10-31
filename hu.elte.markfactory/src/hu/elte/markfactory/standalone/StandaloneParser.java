package hu.elte.markfactory.standalone;

import java.util.Collection;
import java.util.Hashtable;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.FileASTRequestor;

public class StandaloneParser {

	public static void loadFiles(Collection<String> inputFiles,
			FileASTRequestor astRequestor, String[] classPath) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		String[] encodings = new String[inputFiles.size()];
		String[] sourceFiles = new String[inputFiles.size()];
		int i = 0;
		for (String inputFile : inputFiles) {
			encodings[i] = "UTF8";
			sourceFiles[i] = inputFile;
			++i;
		}
		
		
		Hashtable<String,String> options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_SOURCE, "1.6");
		parser.setCompilerOptions(options);
		parser.setEnvironment(classPath, new String[] {}, new String[] {}, true);
		parser.createASTs(sourceFiles, encodings, new String[] {},
				astRequestor, null);
	}

}
