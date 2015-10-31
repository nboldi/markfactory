package hu.elte.markfactory;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;

public class ASTBuilder {

	public ASTBuilder(AST ast) {
		this.ast = ast;
	}

	private AST ast;

	@SuppressWarnings("unchecked")
	public <N extends ASTNode> N copy(N node) {
		return (N) ASTNode.copySubtree(ast, node);
	}
	
	@SuppressWarnings("unchecked")
	public Block newBlock(Statement... stmts) {
		Block newBlock = ast.newBlock();
		newBlock.statements().addAll(Arrays.asList(stmts));
		return newBlock;
	}

	public Type newType(String typeName) {
		return ast.newSimpleType(ast.newName(typeName));
	}

	public SingleVariableDeclaration newVariableDecl(String name, String type) {
		SingleVariableDeclaration exceptionDecl = ast
				.newSingleVariableDeclaration();
		exceptionDecl.setName(ast.newSimpleName(name));
		exceptionDecl.setType(ast.newSimpleType(ast.newName(type)));
		return exceptionDecl;
	}

	public CastExpression newCast(Type type, Expression expr) {
		CastExpression cast = ast.newCastExpression();
		cast.setExpression(expr);
		cast.setType(type);
		return cast;
	}

	public CastExpression newCast(String typeName, Expression expr) {
		return newCast(newType(typeName), expr);
	}

	@SuppressWarnings("unchecked")
	public MethodInvocation newCall(Expression obj, String funName,
			Expression... args) {
		MethodInvocation call = ast.newMethodInvocation();
		call.setExpression(obj);
		call.setName(ast.newSimpleName(funName));
		call.arguments().addAll(Arrays.asList(args));
		return call;
	}

	public ReturnStatement newReturn(Expression expr) {
		ReturnStatement returnStmt = ast.newReturnStatement();
		returnStmt.setExpression(expr);
		return returnStmt;
	}

	public StringLiteral newStringLit(String value) {
		StringLiteral st = ast.newStringLiteral();
		st.setLiteralValue(value);
		return st;
	}

	public CatchClause newCatchClause(String type, String name, Block catchBlock) {
		CatchClause catchClause = ast.newCatchClause();
		catchClause.setException(newVariableDecl(name, type));
		catchClause.setBody(catchBlock);
		return catchClause;
	}

	public TryStatement newTryCatch(Block tryBlock, String type, String name,
			Block catchBlock) {
		return newTryCatch(tryBlock, newCatchClause(type, name, catchBlock));
	}

	@SuppressWarnings("unchecked")
	public TryStatement newTryCatch(Block tryBlock, CatchClause... clauses) {
		TryStatement tryStmt = ast.newTryStatement();
		tryStmt.setBody(tryBlock);
		tryStmt.catchClauses().addAll(Arrays.asList(clauses));
		return tryStmt;
	}

	public Expression newArrayCreation(String canonicalName,
			ArrayInitializer initializer) {
		ArrayCreation ac = ast.newArrayCreation();
		ac.setInitializer(initializer);
		ac.setType(ast.newArrayType(newType(canonicalName)));
		return ac;
	}

	public ArrayCreation newArrayCreation(Type type, Expression... values) {
		return newArrayCreation(type, Arrays.asList(values));
	}

	@SuppressWarnings("unchecked")
	public ArrayCreation newArrayCreation(Type type,
			Collection<Expression> values) {
		ArrayCreation ac = ast.newArrayCreation();
		ArrayInitializer initializer = ast.newArrayInitializer();
		for (Expression expr : values) {
			initializer.expressions().add(expr);
		}
		ac.setInitializer(initializer);
		ac.setType(ast.newArrayType(type));
		return ac;
	}

	public MethodInvocation newStaticCall(String funName, Expression... args) {
		return newCall(null, funName, args);
	}

	public Expression newClassExpr(Type type) {
		TypeLiteral typeLit = ast.newTypeLiteral();
		typeLit.setType(type);
		return typeLit;
	}

	public ASTNode newStaticCall(String className, String funName,
			Expression... args) {
		return newCall(ast.newName(className), funName, args);
	}

}
