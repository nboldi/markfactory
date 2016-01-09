package hu.elte.markfactory.rewrite;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
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

	private abstract class Rewrite {
		int startPosition;
		int endPosition;

		abstract void perform(StringBuilder sb);

		Rewrite(int startPosition, int endPosition) {
			this.startPosition = startPosition;
			this.endPosition = endPosition;
		}
	}

	private class Comment extends Rewrite {
		Comment(int startPosition, int endPosition) {
			super(startPosition, endPosition);
		}

		@Override
		void perform(StringBuilder sb) {
			sb.insert(endPosition, "*/");
			sb.insert(startPosition, "/*");
		}
	}

	private class Remove extends Rewrite {
		Remove(int startPosition, int endPosition) {
			super(startPosition, endPosition);
		}

		@Override
		void perform(StringBuilder sb) {
			sb.delete(startPosition, endPosition);
		}
	}

	// the sections that need to be commented out (map the beginning to the
	// end), in reverse order
	private TreeMap<Integer, Rewrite> rewrites = new TreeMap<>(Comparator.reverseOrder());

	// TODO: merge into annotationDetector
	private static List<String> annotationsToRemove = Arrays.asList(TestSolution.class.getCanonicalName(),
			DummyChild.class.getCanonicalName(), ExamTest.class.getCanonicalName(),
			ExamExercise.class.getCanonicalName());

	private AnnotationDetector annotationDetector = new AnnotationDetector();

	@Override
	public boolean visit(ImportDeclaration node) {
		if (annotationsToRemove.contains(((ITypeBinding) node.resolveBinding()).getQualifiedName())) {
			registerNodeRemove(node);
		}
		if (annotationDetector.isTestSolution(node.resolveBinding())) {
			commentNode(node);
		}
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if (!annotationDetector.isTestClass(node)) {
			return false;
		}
		if (removeAnnots(node.modifiers())) {
			rewriteDoneInCompUnit = true;
		}
		// if (annotationDetector.isDummyChild(node.resolveBinding())) {
		// commentNode(node);
		// }
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
		ASTNode nextNode = getNextNode(node);
		if (nextNode != null) {
			rewrites.put(node.getStartPosition(), new Remove(node.getStartPosition(), nextNode.getStartPosition()));
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
		// assumption: structural properties are in order in which they appear
		// in the source file
		List<?> structuralProperties = node.getParent().structuralPropertiesForType();
		@SuppressWarnings("unchecked")
		List<ASTNode> children = (List<ASTNode>) parent.getStructuralProperty(node.getLocationInParent());
		int nextIndex = children.indexOf(node) + 1;
		if (children.size() > nextIndex) {
			return children.get(nextIndex);
		} else {
			int propIndex = structuralProperties.indexOf(node.getLocationInParent());
			for (int i = propIndex + 1; i < structuralProperties.size(); i++) {
				StructuralPropertyDescriptor propertyDescr = (StructuralPropertyDescriptor) structuralProperties.get(i);
				Object propValues = parent.getStructuralProperty(propertyDescr);
				if (propertyDescr.isChildListProperty() && !((List<?>) propValues).isEmpty()) {
					return ((List<ASTNode>) propValues).get(0);
				} else if (propertyDescr.isChildProperty() && propValues != null) {
					return (ASTNode) propValues;
				}
			}
			return getNextNode(parent);
		}
	}

	/**
	 * Comments out a given AST node.
	 */
	private void commentNode(ASTNode node) {
		int start = node.getStartPosition();
		int startPos = start;
		rewrites.put(startPos, new Comment(startPos, start + node.getLength()));
	}

	/**
	 * Comments out the statements in the method body.
	 */
	private void commentBody(ASTNode node) {
		int start = node.getStartPosition();
		int startPos = start + 1;
		rewrites.put(startPos, new Comment(startPos, start + node.getLength() - 1));
	}

	private boolean removeAnnots(List<?> modifiers) {
		boolean[] removed = new boolean[] { false };
		modifiers.forEach(mod -> {
			if (modifierShouldBeRemoved(mod)) {
				registerNodeRemove((ASTNode) mod);
				removed[0] = true;
			}
		});
		return removed[0];
	}

	private boolean modifierShouldBeRemoved(Object mod) {
		if (!(mod instanceof Annotation)) {
			return false;
		}
		IAnnotationBinding annBinding = ((Annotation) mod).resolveAnnotationBinding();
		return mod instanceof Annotation && annBinding != null
				&& annotationsToRemove.contains(annBinding.getAnnotationType().getQualifiedName().toString());
	}

	@Override
	public String runOn(CompilationUnit root, Document document) throws MalformedTreeException, BadLocationException {
		String res = super.runOn(root, document);
		if (res != null) {
			StringBuilder sb = new StringBuilder(res);
			// from the end of the file to the beginning
			// (no need to consider the effect of rewriting on the remaining
			// positions)
			for (Rewrite rewrite : rewrites.values()) {
				rewrite.perform(sb);
			}
			return sb.toString();
		}
		return res;
	}

}
