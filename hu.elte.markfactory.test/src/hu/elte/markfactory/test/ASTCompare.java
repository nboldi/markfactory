package hu.elte.markfactory.test;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class ASTCompare {

	@SuppressWarnings("unchecked")
	static boolean equals(ASTNode left, ASTNode right) {
		// if both are null, they are equal, but if only one, they aren't
		if (left == null && right == null) {
			return true;
		} else if (left == null || right == null) {
			return false;
		}
		// if node types are the same we can assume that they will have the same
		// properties
		if (left.getNodeType() != right.getNodeType()) {
			return false;
		}
		List<StructuralPropertyDescriptor> props = left
				.structuralPropertiesForType();
		for (StructuralPropertyDescriptor property : props) {
			Object leftVal = left.getStructuralProperty(property);
			Object rightVal = right.getStructuralProperty(property);
			if (property.isSimpleProperty()) {
				// check for simple properties (primitive types, Strings, ...)
				// with normal equality
				if (!leftVal.equals(rightVal)) {
					return false;
				}
			} else if (property.isChildProperty()) {
				// recursively call this function on child nodes
				if (!equals((ASTNode) leftVal, (ASTNode) rightVal)) {
					return false;
				}
			} else if (property.isChildListProperty()) {
				Iterator<ASTNode> leftValIt = ((Iterable<ASTNode>) leftVal)
						.iterator();
				Iterator<ASTNode> rightValIt = ((Iterable<ASTNode>) rightVal)
						.iterator();
				while (leftValIt.hasNext() && rightValIt.hasNext()) {
					// recursively call this function on child nodes
					if (!equals(leftValIt.next(), rightValIt.next())) {
						return false;
					}
				}
				// one of the value lists have additional elements
				if (leftValIt.hasNext() || rightValIt.hasNext()) {
					return false;
				}
			}
		}
		return true;
	}

}
