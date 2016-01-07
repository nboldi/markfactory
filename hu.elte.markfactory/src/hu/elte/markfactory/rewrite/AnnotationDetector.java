package hu.elte.markfactory.rewrite;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import hu.elte.markfactory.annotations.DummyChild;
import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;
import hu.elte.markfactory.annotations.TestSolution;

public class AnnotationDetector {

	public boolean isExamExercise(MethodDeclaration node) {
		return hasAnnotation(ExamExercise.class, node.resolveBinding());
	}

	public boolean isTestSolution(IBinding typ) {
		return hasAnnotation(TestSolution.class, typ) || hasAnnotation(DummyChild.class, typ);
	}

	public boolean isDummyChild(IBinding typ) {
		return hasAnnotation(DummyChild.class, typ);
	}
	
	public boolean isTestClass(TypeDeclaration node) {
		return hasAnnotation(ExamTest.class, node.resolveBinding());
	}

	private static boolean hasAnnotation(Class<?> annotation, IBinding annotated) {
		boolean hasAnnotation = false;
		if (annotated != null && annotated.getAnnotations() != null) {
			for (IAnnotationBinding annot : annotated.getAnnotations()) {
				if (annot.getAnnotationType().getQualifiedName().equals(annotation.getName())) {
					hasAnnotation = true;
				}
			}
		}
		return hasAnnotation;
	}

}
