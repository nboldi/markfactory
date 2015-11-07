package hu.elte.markfactory.rewrite;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public abstract class ModificationRecordingVisitor extends ASTVisitor {

	protected boolean rewriteDoneInCompUnit = false;
	protected AST ast;

	public ModificationRecordingVisitor(AST ast) {
		this.ast = ast;
	}

	public ModificationRecordingVisitor(boolean visitDocTags) {
		super(visitDocTags);
	}

	public boolean didRewrite() {
		return rewriteDoneInCompUnit;
	}
	
	public String runOn(CompilationUnit root, Document document) throws MalformedTreeException, BadLocationException {
		root.accept(this);
		if (didRewrite()) {
			TextEdit edits = root.rewrite(document, null);
			edits.apply(document);
			return document.get();
		}
		return null;
	}

	
	
}