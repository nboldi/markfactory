package hu.elte.markfactory.rewrite;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import hu.elte.markfactory.annotations.DummyChild;
import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;
import hu.elte.markfactory.annotations.TestSolution;

public class PlainTestVisitor extends ASTVisitor {

	private static List<String> annotationsToRemove = Arrays.asList(TestSolution.class.getCanonicalName(),
			DummyChild.class.getCanonicalName(), ExamTest.class.getCanonicalName(),
			ExamExercise.class.getCanonicalName());

	@Override
	public boolean visit(TypeDeclaration node) {
		return removeAnnots(node.modifiers());
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		return removeAnnots(node.modifiers());
	}

	private boolean removeAnnots(List<?> modifiers) {
		return modifiers.removeIf(mod -> mod instanceof Annotation
				&& annotationsToRemove.contains(((Annotation) mod).getTypeName().toString()));
	}

}
