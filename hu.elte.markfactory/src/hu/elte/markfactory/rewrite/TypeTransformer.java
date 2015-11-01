package hu.elte.markfactory.rewrite;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;
import hu.elte.markfactory.ASTBuilder;

public class TypeTransformer {

	private AST ast;
	private ASTBuilder builder;
	private AnnotationDetector annotationDetector;

	public TypeTransformer(AST ast, ASTBuilder builder, AnnotationDetector annotationDetector) {
		this.ast = ast;
		this.builder = builder;
		this.annotationDetector = annotationDetector;
	}

	public Optional<Type> cleanType(Type type) {
		return cleanType(type, type.resolveBinding());
	}

	@SuppressWarnings("unchecked")
	public Optional<Type> cleanType(Type type, ITypeBinding typeBnd) {
		if (typeBnd != null && annotationDetector.isTestSolution(typeBnd)) {
			return Optional.of(mostSpecificReplacement(typeBnd));
		}
		boolean changed = false;
		Type copy = (Type) ASTNode.copySubtree(ast, type);
		if (type.isParameterizedType()) {
			ParameterizedType pType = (ParameterizedType) type;
			ParameterizedType pCopy = (ParameterizedType) copy;
			List<Object> typeArguments = pType.typeArguments();

			for (int i = 0; i < typeArguments.size(); i++) {
				final int ind = i;
				Optional<Type> cleanTypeParam = cleanType((Type) typeArguments.get(ind),
						typeBnd.getTypeArguments()[ind]);
				cleanTypeParam.ifPresent(t -> pCopy.typeArguments().set(ind, t));
				changed |= cleanTypeParam.isPresent();
			}
		}
		if (type.isArrayType()) {
			ArrayType at = (ArrayType) type;
			ArrayType aCopy = (ArrayType) copy;
			Optional<Type> cleanElemType = cleanType(at.getElementType(), typeBnd.getElementType());
			cleanElemType.ifPresent(t -> aCopy.setElementType(t));
			changed |= cleanElemType.isPresent();
		}
		if (changed) {
			return Optional.of(copy);
		} else {
			return Optional.empty();
		}
	}

	public SimpleType mostSpecificReplacement(ITypeBinding iTypeBinding) {
		for (ITypeBinding ancestor : ancestors(iTypeBinding)) {
			if (!annotationDetector.isTestSolution(ancestor)) {
				return ast.newSimpleType(ast.newSimpleName(ancestor.getName()));
			}
		}
		return ast.newSimpleType(ast.newName(Object.class.getName()));
	}

	private static List<ITypeBinding> ancestors(ITypeBinding type) {
		ITypeBinding superType = type.getSuperclass();
		List<ITypeBinding> ret = new LinkedList<>();
		while (superType != null) {
			ret.add(superType);
			superType = superType.getSuperclass();
		}
		return ret;
	}

	public Type typeFromBinding(ITypeBinding typeBinding) {
		if (typeBinding.isPrimitive()) {
			return ast.newPrimitiveType(PrimitiveType.toCode(typeBinding.getName()));
		}

		if (typeBinding.isCapture()) {
			ITypeBinding wildCard = typeBinding.getWildcard();
			WildcardType capType = ast.newWildcardType();
			ITypeBinding bound = wildCard.getBound();
			if (bound != null) {
				capType.setBound(typeFromBinding(bound), wildCard.isUpperbound());
			}
			return capType;
		}

		if (typeBinding.isArray()) {
			Type elType = typeFromBinding(typeBinding.getElementType());
			return ast.newArrayType(elType, typeBinding.getDimensions());
		}

		if (typeBinding.isParameterizedType()) {
			ParameterizedType type = ast.newParameterizedType(typeFromBinding(typeBinding.getErasure()));

			@SuppressWarnings("unchecked")
			List<Type> newTypeArgs = type.typeArguments();
			for (ITypeBinding typeArg : typeBinding.getTypeArguments()) {
				newTypeArgs.add(typeFromBinding(typeArg));
			}

			return type;
		}

		// simple or raw type
		String qualName = typeBinding.getQualifiedName();
		if ("".equals(qualName)) {
			throw new IllegalArgumentException("No name for type binding.");
		}
		return ast.newSimpleType(ast.newName(qualName));
	}

	public Type box(Type type) {
		if (type instanceof PrimitiveType) {
			Code primitiveTypeCode = ((PrimitiveType) type).getPrimitiveTypeCode();
			if (primitiveTypeCode.equals(PrimitiveType.BOOLEAN)) {
				return builder.newType(Boolean.class.getCanonicalName());
			} else if (primitiveTypeCode.equals(PrimitiveType.CHAR)) {
				return builder.newType(Character.class.getCanonicalName());
			} else if (primitiveTypeCode.equals(PrimitiveType.INT)) {
				return builder.newType(Integer.class.getCanonicalName());
			} else if (primitiveTypeCode.equals(PrimitiveType.SHORT)) {
				return builder.newType(Short.class.getCanonicalName());
			} else if (primitiveTypeCode.equals(PrimitiveType.BYTE)) {
				return builder.newType(Byte.class.getCanonicalName());
			} else if (primitiveTypeCode.equals(PrimitiveType.LONG)) {
				return builder.newType(Long.class.getCanonicalName());
			} else if (primitiveTypeCode.equals(PrimitiveType.FLOAT)) {
				return builder.newType(Float.class.getCanonicalName());
			} else if (primitiveTypeCode.equals(PrimitiveType.DOUBLE)) {
				return builder.newType(Double.class.getCanonicalName());
			}
		}
		return type;
	}

}
