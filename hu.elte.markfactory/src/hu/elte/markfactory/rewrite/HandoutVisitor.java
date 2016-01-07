package hu.elte.markfactory.rewrite;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;

import hu.elte.markfactory.annotations.DummyChild;
import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;
import hu.elte.markfactory.annotations.TestSolution;

/**
 * This visitor rewrites the AST to be useful as a handout to students. All the
 * test-system-specific annotations are removed and all the bodies of the
 * exercises are commented out.
 */
public class HandoutVisitor extends ModificationRecordingVisitor {

	public HandoutVisitor(AST ast) {
		super(ast);
	}

	// the sections that need to be commented out (map the beginning to the
	// end), in reverse order
	private TreeMap<Integer, Integer> commented = new TreeMap<>(Comparator.reverseOrder());
	private int removedChars = 0;

	// TODO: merge into annotationDetector
	private static List<String> annotationsToRemove = Arrays.asList(TestSolution.class.getCanonicalName(),
			DummyChild.class.getCanonicalName(), ExamTest.class.getCanonicalName(),
			ExamExercise.class.getCanonicalName());

	private AnnotationDetector annotationDetector = new AnnotationDetector();

	@Override
	public boolean visit(ImportDeclaration node) {
		if (annotationsToRemove.contains(((ITypeBinding) node.resolveBinding()).getQualifiedName())) {
			registerNodeRemove(node);
			node.delete();
		}
		if (annotationDetector.isTestSolution(node.resolveBinding())) {
			commentNode(node);
		}
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if (removeAnnots(node.modifiers())) {
			rewriteDoneInCompUnit = true;
		}
//		if (annotationDetector.isDummyChild(node.resolveBinding())) {
//			commentNode(node);
//		}
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if (removeAnnots(node.modifiers())) {
			rewriteDoneInCompUnit = true;
			commentBody(node.getBody());
		}
		return true;
	}
	
	@Override
	public boolean visit(FieldDeclaration node) {
		if (annotationDetector.isTestSolution(node.getType().resolveBinding())) {
			commentNode(node);
		}
		return true;
	}

	/**
	 * Registers the removal of the node. This is necessary because otherwise it
	 * would invalidate the character numbers of the code sections that need to
	 * be commented out.
	 */
	private void registerNodeRemove(ASTNode node) {
		if (getNextNode(node) != null) {
			removedChars += getNextNode(node).getStartPosition() - node.getStartPosition();
		}
	}

	/**
	 * Gets the next node in the AST.
	 */
	private static ASTNode getNextNode(ASTNode node) {
		while (node.getLocationInParent() != null && !node.getLocationInParent().isChildListProperty()) {
			node = node.getParent();
		}
		if (node.getLocationInParent() == null) {
			return null;
		}
		ASTNode parent = node.getParent();
		@SuppressWarnings("unchecked")
		List<ASTNode> children = (List<ASTNode>) parent.getStructuralProperty(node.getLocationInParent());
		int nextIndex = children.indexOf(node) + 1;
		if (children.size() > nextIndex) {
			return children.get(nextIndex);
		} else {
			return getNextNode(parent);
		}
	}

	/**
	 * Comments out a given AST node.
	 */
	private void commentNode(ASTNode node) {
		int start = node.getStartPosition();
		commented.put(start - removedChars, start + node.getLength() - removedChars);
	}

	/**
	 * Comments out the statements in the method body.
	 */
	private void commentBody(ASTNode node) {
		int start = node.getStartPosition();
		commented.put(start - removedChars + 1, start + node.getLength() - removedChars - 1);
	}

	private boolean removeAnnots(List<?> modifiers) {
		modifiers.forEach(mod -> {
			if (modifierShouldBeRemoved(mod)) {
				registerNodeRemove((ASTNode) mod);
			}
		});
		return modifiers.removeIf(this::modifierShouldBeRemoved);
	}

	private boolean modifierShouldBeRemoved(Object mod) {
		return mod instanceof Annotation && annotationsToRemove.contains(
				((Annotation) mod).resolveAnnotationBinding().getAnnotationType().getQualifiedName().toString());
	}

	@Override
	public String runOn(CompilationUnit root, Document document) throws MalformedTreeException, BadLocationException {
		String res = super.runOn(root, document);
		if (res != null) {
			StringBuilder sb = new StringBuilder(res);
			// from the end of the file to the beginning
			// (no need to consider the effect of rewriting on the remaining
			// positions)
			for (Entry<Integer, Integer> commentedSection : commented.entrySet()) {
				sb.insert(commentedSection.getValue(), "*/");
				sb.insert(commentedSection.getKey(), "/*");
			}
			return sb.toString();
		}
		return res;
	}

}
