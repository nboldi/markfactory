package hu.elte.markfactory.rewrite;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import hu.elte.markfactory.ASTBuilder;
import hu.elte.markfactory.TranslationException;
import hu.elte.markfactory.annotations.ExamExercise;
import hu.elte.markfactory.annotations.ExamTest;
import hu.elte.markfactory.testbase.MissingProgramElementException;
import hu.elte.markfactory.testbase.ReflectionTester;

@SuppressWarnings("unchecked")
public class AutocheckVisitor extends ModificationRecordingVisitor {

	private ASTBuilder builder;

	private AnnotationDetector annotationDetector = new AnnotationDetector();
	private TypeTransformer typeTransformer;

	public AutocheckVisitor(AST ast) {
		super(ast);
		builder = new ASTBuilder(ast);
		typeTransformer = new TypeTransformer(ast, builder, annotationDetector);
	}

	@Override
	public boolean visit(CompilationUnit node) {
		rewriteDoneInCompUnit = false;
		return true;
	}

	/**
	 * Deletes imports that reference parts of the sample solution.
	 */
	@Override
	public void endVisit(CompilationUnit node) {
		boolean isTest = checkIfTestCompilation(node);
		if (isTest) {
			node.imports().removeIf(id -> {
				return annotationDetector.isTestSolution(((ImportDeclaration) id).resolveBinding());
			});
		}
	}

	private boolean checkIfTestCompilation(CompilationUnit node) {
		boolean isTest = false;
		for (Object type : node.types()) {
			if (annotationDetector.isTestClass((TypeDeclaration) type)) {
				isTest = true;
			} else {
				if (isTest) {
					throw new TranslationException(
							"There are test and non-test types in the same compilation unit: " + node);
				}
			}
		}
		return isTest;
	}

	/**
	 * Traverses classes that are annotated by {@link ExamTest}. A new
	 * supertype, {@link ReflectionTester} is added to these classes.
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		if (!annotationDetector.isTestClass(node)) {
			return false;
		}
		rewriteDoneInCompUnit = true;
		// TODO: place a marker if class already has a supertype
		node.setSuperclassType(builder.newType(ReflectionTester.class.getName()));
		return true;
	}

	/**
	 * Generalizes methods that return one of the solution classes to have a
	 * more generic return type.
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		typeTransformer.cleanType(node.getReturnType2()).ifPresent(t -> node.setReturnType2(t));
		return annotationDetector.isExamExercise(node);
	}

	/**
	 * Wraps {@linkplain ExamExercise} methods into try-catch blocks.
	 */
	@Override
	public void endVisit(MethodDeclaration node) {
		if (annotationDetector.isExamExercise(node)) {
			wrapBodyInTryCatchBlock(node);
		}
	}

	private void wrapBodyInTryCatchBlock(MethodDeclaration node) {
		MethodInvocation traceCall = builder.newCall(ast.newSimpleName("e"), "printStackTrace");
		ReturnStatement returnStmt = builder.newReturn(createExpressionForType(node.getReturnType2()));
		Block tryBlock = builder.copy(node.getBody());
		Block print = builder.newBlock(
				ast.newExpressionStatement(builder.newStaticCall("output", ast.newSimpleName("e"))), returnStmt);
		Block trace = builder.newBlock(ast.newExpressionStatement(traceCall), builder.copy(returnStmt));
		TryStatement tryStmt = builder.newTryCatch(tryBlock,
				builder.newCatchClause(MissingProgramElementException.class.getCanonicalName(), "e", print),
				builder.newCatchClause(Throwable.class.getCanonicalName(), "e", trace));
		node.setBody(builder.newBlock(tryStmt));
	}

	/**
	 * Creates a default literal for a given type. Returns null if no return
	 * needed.
	 */
	private Expression createExpressionForType(Type type) {
		ITypeBinding binding = type.resolveBinding();
		if (binding == null) {
			// generated node, must be a class
			return ast.newNullLiteral();
		}
		switch (binding.getName()) {
		case "void":
			return null;
		case "boolean":
			return ast.newBooleanLiteral(false);
		case "char":
			CharacterLiteral literal = ast.newCharacterLiteral();
			literal.setCharValue('\0');
			return literal;
		case "byte":
		case "int":
		case "short":
		case "long":
		case "float":
		case "double":
			return ast.newNumberLiteral();
		default:
			return ast.newNullLiteral();
		}
	}

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		if (annotationDetector.isTestSolution(node.getType().resolveBinding())) {
			node.setType(typeTransformer.mostSpecificReplacement(node.getType().resolveBinding()));
		}
		typeTransformer.cleanType(node.getType()).ifPresent(t -> node.setType(t));
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationExpression node) {
		if (annotationDetector.isTestSolution(node.resolveTypeBinding())) {
			SimpleType replacement = typeTransformer.mostSpecificReplacement(node.resolveTypeBinding());
			node.setType(replacement);
		}
		typeTransformer.cleanType(node.getType()).ifPresent(t -> node.setType(t));
		return super.visit(node);
	}

	/**
	 * Replaces fields that have a solution class type to have a more generic
	 * type.
	 */
	@Override
	public boolean visit(FieldDeclaration node) {
		Type newType = node.getType();
		if (annotationDetector.isTestSolution(newType.resolveBinding())) {
			SimpleType replacement = typeTransformer.mostSpecificReplacement(newType.resolveBinding());
			node.setType(replacement);
		}
		typeTransformer.cleanType(node.getType()).ifPresent(t -> node.setType(t));
		return super.visit(node);
	}

	/**
	 * Replaces local variables that have a solution class type to have a more
	 * generic type.
	 */
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		if (annotationDetector.isTestSolution(node.getType().resolveBinding())) {
			SimpleType replacement = typeTransformer.mostSpecificReplacement(node.getType().resolveBinding());
			node.setType(replacement);
		}
		typeTransformer.cleanType(node.getType()).ifPresent(t -> node.setType(t));
		return super.visit(node);
	}

	@Override
	public void endVisit(ArrayCreation node) {
		if (typeTransformer.cleanType(node.getType()).isPresent()) {
			ArrayType nodeType = node.getType();
			Type elementType = nodeType.getElementType();
			ITypeBinding typeBnd = elementType.resolveBinding();

			ArrayInitializer initializer = (ArrayInitializer) ASTNode.copySubtree(ast, node.getInitializer());
			ArrayType objArrayType = ast.newArrayType(builder.newType(Object.class.getCanonicalName()));
			Expression newArrayCreation = builder.newArrayCreation(Object.class.getCanonicalName(), initializer);
			replaceNode(node, builder.newCast(objArrayType, builder.newStaticCall("createArray",
					builder.newStringLit(typeBnd.getQualifiedName()), newArrayCreation)));
		}
	}

	@Override
	public boolean visit(Assignment node) {
		trasformAssignment(node.getLeftHandSide(), node.getRightHandSide())
				.ifPresent(newAssign -> replaceNode(node, newAssign));
		return true;
	}

	/**
	 * Transform an assignment if the left hand side is a field access. Does not
	 * do anything for assignments where the left side only contains a field
	 * access, like {@code this.field[0] = ...}, these expressions will be
	 * resolved by replacing the {@code this.field} expression to a
	 * {@code getField} call.
	 */
	public Optional<Expression> trasformAssignment(Expression leftSide, Expression rightSide) {
		if (leftSide instanceof FieldAccess) {
			FieldAccess fieldAccess = (FieldAccess) leftSide;
			return Optional.of(genFieldSet(fieldAccess.resolveFieldBinding(), fieldAccess.getExpression(), rightSide));
		} else if (leftSide instanceof QualifiedName) {
			QualifiedName qNameField = (QualifiedName) leftSide;
			return Optional.ofNullable(
					genFieldSet((IVariableBinding) qNameField.resolveBinding(), qNameField.getQualifier(), rightSide));
		} else if (leftSide instanceof SuperFieldAccess) {
			throw new TranslationException("Super field access is not supported.");
		}
		return Optional.empty();
	}

	private Expression genFieldSet(IVariableBinding binding, Expression base, Expression rightSide) {
		if (!annotationDetector.isTestSolution(base.resolveTypeBinding())) {
			return null;
		}
		if ((binding.getModifiers() & Modifier.STATIC) != 0) {
			return builder.newStaticCall("staticFieldSet",
					builder.newStringLit(binding.getDeclaringClass().getQualifiedName()),
					builder.newStringLit(binding.getName()), builder.copy(rightSide));
		} else {
			return builder.newStaticCall("fieldSet", builder.copy(base), builder.newStringLit(binding.getName()),
					builder.copy(rightSide));
		}
	}

	@Override
	public void endVisit(FieldAccess node) {
		if (annotationDetector.isTestSolution(node.getExpression().resolveTypeBinding())) {
			replaceNode(node, genFieldGet(node.resolveFieldBinding(), node.getExpression()));
		}
	}

	@Override
	public boolean visit(QualifiedName node) {
		IBinding binding = node.resolveBinding();
		if (binding == null || !(binding instanceof IVariableBinding)) {
			return true;
		}
		IVariableBinding varBinding = (IVariableBinding) binding;
		if (!varBinding.isField()) {
			return true;
		}
		if (annotationDetector.isTestSolution(node.getQualifier().resolveTypeBinding())) {
			replaceNode(node, genFieldGet(varBinding, node.getQualifier()));
		}
		return true;
	}

	private Expression genFieldGet(IVariableBinding binding, Expression base) {
		Expression get;
		if ((binding.getModifiers() & Modifier.STATIC) != 0) {
			get = builder.newStaticCall("staticFieldValue",
					builder.newStringLit(binding.getDeclaringClass().getQualifiedName()),
					builder.newStringLit(binding.getName()));
		} else {
			get = builder.newStaticCall("fieldValue", builder.copy(base), builder.newStringLit(binding.getName()));
		}
		ITypeBinding typeBinding = binding.getType();
		Type newType = annotationDetector.isTestSolution(typeBinding)
				? typeTransformer.mostSpecificReplacement(typeBinding)
				: typeTransformer.box(typeTransformer.typeFromBinding(typeBinding));
		return builder.paren(builder.newCast(newType, get));
	}

	/**
	 * Replaces normal method calls with reflection calls.
	 */
	@Override
	public void endVisit(MethodInvocation node) {
		IMethodBinding binding = node.resolveMethodBinding();
		if (binding != null && annotationDetector.isTestSolution(binding.getDeclaringClass())) {
			handleMethodTypeArgs(node.typeArguments());
			MethodInvocation newCall = createReflCall(node);
			if (binding.getReturnType().getName().equals("void")) {
				replaceNode(node, newCall);
				return;
			}
			CastExpression cast = ast.newCastExpression();
			Type bindingType = typeTransformer.typeFromBinding(binding.getReturnType());
			cast.setType(typeTransformer.cleanType(bindingType, binding.getReturnType()).orElse(bindingType));
			cast.setExpression(newCall);
			replaceNode(node, wrapExpression(cast, node.getParent()));
		}
	}

	private static void replaceNode(ASTNode node, ASTNode newNode) {
		ASTNode parent = node.getParent();
		StructuralPropertyDescriptor loc = node.getLocationInParent();
		if (loc.isChildListProperty()) {
			List<Object> siblings = (List<Object>) parent.getStructuralProperty(loc);
			siblings.set(siblings.indexOf(node), newNode);
		} else {
			parent.setStructuralProperty(loc, newNode);
		}
	}

	private void handleMethodTypeArgs(List<Object> typeArguments) {
		for (int i = 0; i < typeArguments.size(); ++i) {
			final int ind = i;
			typeTransformer.cleanType((Type) typeArguments.get(i)).ifPresent(t -> typeArguments.set(ind, t));
		}
	}

	private MethodInvocation createReflCall(MethodInvocation original) {
		MethodInvocation newCall = ast.newMethodInvocation();

		StringLiteral methodName = ast.newStringLiteral();
		methodName.setLiteralValue(original.getName().getFullyQualifiedName());

		IMethodBinding methodBind = original.resolveMethodBinding();
		boolean isStatic = 0 < (methodBind.getModifiers() & Modifier.STATIC);
		if (isStatic) {
			newCall.setName(ast.newSimpleName("staticCall"));
			StringLiteral className = ast.newStringLiteral();
			className.setLiteralValue(methodBind.getDeclaringClass().getErasure().getQualifiedName());
			newCall.arguments().add(className);
			newCall.arguments().add(methodName);
		} else {
			newCall.setName(ast.newSimpleName("call"));
			newCall.arguments().add(methodName);
			newCall.arguments().add(ASTNode.copySubtree(ast, original.getExpression()));
		}
		List<Expression> formalTypes = new LinkedList<>();
		for (ITypeBinding type : methodBind.getParameterTypes()) {
			formalTypes.add(builder.newStringLit(type.getErasure().getQualifiedName()));
		}
		newCall.arguments().add(builder.newArrayCreation(builder.newType("java.lang.String"), formalTypes));
		LinkedList<Expression> newArgs = new LinkedList<>(original.arguments());
		newArgs.replaceAll(a -> (Expression) ASTNode.copySubtree(ast, a));
		newCall.arguments().add(builder.newArrayCreation(builder.newType("java.lang.Object"), newArgs));

		return newCall;
	}

	public boolean visit(InstanceofExpression node) {
		String typeName = node.getRightOperand().resolveBinding().getErasure().getQualifiedName();
		MethodInvocation loadClass = builder.newCall(null, "loadClass", builder.newStringLit(typeName));
		MethodInvocation newCall = builder.newCall(loadClass, "isInstance",
				(Expression) ASTNode.copySubtree(ast, node.getLeftOperand()));
		replaceNode(node, newCall);
		return true;
	};

	/**
	 * Replaces normal constructor calls with calling the
	 * {@link ReflectionTester}.construct method.
	 */
	@Override
	public void endVisit(ClassInstanceCreation node) {

		IMethodBinding ctorBinding = node.resolveConstructorBinding();
		if (ctorBinding == null) {
			System.err.println(
					"Constructor binding cannot be resolved for: " + node + ". Please don't use diamond patterns.");
			return;
		}
		ITypeBinding constructedType = ctorBinding.getDeclaringClass();
		typeTransformer.cleanType(node.getType()).ifPresent(t -> node.setType(t));
		if (annotationDetector.isTestSolution(constructedType)) {
			Expression newCall = createReflConstructorCall(node);
			CastExpression cast = ast.newCastExpression();
			cast.setType(typeTransformer.mostSpecificReplacement(constructedType));
			cast.setExpression(newCall);
			replaceNode(node, wrapExpression(cast, node.getParent()));
		}
	}

	private Expression createReflConstructorCall(ClassInstanceCreation node) {
		IMethodBinding ctorBinding = node.resolveConstructorBinding();
		MethodInvocation newCall = ast.newMethodInvocation();
		newCall.setName(ast.newSimpleName("construct"));
		StringLiteral className = ast.newStringLiteral();
		className.setLiteralValue(ctorBinding.getDeclaringClass().getErasure().getQualifiedName());
		newCall.arguments().add(className);
		LinkedList<Expression> newArgs = new LinkedList<>(node.arguments());
		newArgs.replaceAll(a -> (Expression) ASTNode.copySubtree(ast, a));
		List<Expression> formalTypes = new LinkedList<>();
		for (ITypeBinding ctorType : ctorBinding.getParameterTypes()) {
			formalTypes.add(builder.newStringLit(ctorType.getErasure().getQualifiedName()));
		}
		newCall.arguments().add(builder.newArrayCreation(builder.newType("java.lang.String"), formalTypes));
		newCall.arguments().add(builder.newArrayCreation(builder.newType("java.lang.Object"), newArgs));
		return newCall;
	}

	private Expression wrapExpression(Expression expr, ASTNode parentToBe) {
		if (expr.getNodeType() == ASTNode.CAST_EXPRESSION) {
			if (parentToBe.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
				MethodInvocation invocation = ast.newMethodInvocation();
				invocation.setName(ast.newSimpleName("skip"));
				invocation.arguments().add(expr);
				return invocation;
			} else {
				ParenthesizedExpression parens = ast.newParenthesizedExpression();
				parens.setExpression(expr);
				return parens;
			}
		} else {
			return expr;
		}
	}

	@Override
	public boolean visit(TypeLiteral node) {
		String typeName = node.getType().resolveBinding().getQualifiedName();
		ASTNode classofExpr = builder.newStaticCall("loadClass", builder.newStringLit(typeName));
		replaceNode(node, classofExpr);
		return true;
	}

}
