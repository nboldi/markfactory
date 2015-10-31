package hu.elte.markfactory.builders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import hu.elte.markfactory.annotations.ExamTest;
import hu.elte.markfactory.rewrite.AutocheckVisitor;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class ExamTestBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "hu.elte.markfactory.examtestbuilder";

	@Override
	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) throws CoreException {

		final List<ICompilationUnit> compUnits = new LinkedList<>();
		if (kind == AUTO_BUILD || kind == INCREMENTAL_BUILD) {
			IResourceDelta delta = getDelta(getProject());
			delta.accept(new IResourceDeltaVisitor() {

				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					addResourceAsCompUnit(compUnits, delta.getResource());
					return true;
				}
			});
		} else if (kind == FULL_BUILD) {
			getProject().accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					addResourceAsCompUnit(compUnits, resource);
					return true;
				}
			});
		}
		for (ICompilationUnit compUnit : compUnits) {
			if (hasTestClass(compUnit))
				processClass(compUnit);
		}
		return null;
	}

	private boolean hasTestClass(ICompilationUnit compUnit)
			throws JavaModelException {
		if (!compUnit.isOpen())
			return false;
		for (IType type : compUnit.getAllTypes()) {
			if (checkAnnotation(type, ExamTest.class))
				return true;
		}
		return false;
	}

	private boolean checkAnnotation(IJavaElement elem, Class<?> annot)
			throws JavaModelException {
		if (!(elem instanceof IAnnotatable))
			return false;
		IAnnotation[] annotations = ((IAnnotatable) elem).getAnnotations();
		for (IAnnotation annotation : annotations) {
			if (annotation.getElementName().equals(annot.getSimpleName()))
				return true;
		}
		return false;
	}

	private void processClass(ICompilationUnit compUnit)
			throws JavaModelException {
		generateModifiedFile(compUnit);
	}

	private void generateModifiedFile(ICompilationUnit compUnit) {
		String destFile = "";
		try {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setSource(compUnit);
			parser.setResolveBindings(true);
			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
			AST ast = astRoot.getAST();

			astRoot.recordModifications();

			Document document = new Document(compUnit.getSource());

			AutocheckVisitor visitor = new AutocheckVisitor(ast);
			astRoot.accept(visitor);
			if (visitor.didRewrite()) {
				TextEdit edits = astRoot.rewrite(document, null);
				edits.apply(document);
				String newSource = document.get();
				destFile = writeOutResult(compUnit, newSource);
			}

		} catch (MalformedTreeException | BadLocationException | CoreException e) {
			System.err.println("Source transformation failed:");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Source transformation failed for '" + destFile
					+ "':");
			e.printStackTrace();
		}
	}

	private String writeOutResult(ICompilationUnit compUnit, String newSource)
			throws IOException, FileNotFoundException, CoreException {
		IProject project = compUnit.getResource().getProject();
		IPath originalPath = compUnit.getResource().getProjectRelativePath();

		// TODO: put into another project instead
		IPath newPath = originalPath
				.removeLastSegments(1)
				.append(originalPath.removeFileExtension().lastSegment()
						+ "Refl")
				.addFileExtension(originalPath.getFileExtension());
		File outFile = project.getLocation().append(newPath).toFile();
		if (!outFile.exists()) {
			outFile.getParentFile().mkdirs();
			outFile.createNewFile();
		}
		PrintWriter out = new PrintWriter(outFile);
		out.println(newSource);
		out.close();

		project.refreshLocal(IProject.DEPTH_INFINITE, null);
		project.findMember(newPath).setDerived(true, null);
		return outFile.getAbsolutePath();
	}

	private void addResourceAsCompUnit(final List<ICompilationUnit> compUnits,
			IResource resource) {
		if (resource.isAccessible() && !resource.isDerived()
				&& resource.getType() == IResource.FILE) {
			ICompilationUnit compUnit = null;
			try {
				IFile file = (IFile) resource;
				if (file.getFullPath().getFileExtension().equals("java"))
					compUnit = JavaCore.createCompilationUnitFrom(file);
			} catch (IllegalArgumentException e) {
			}
			if (compUnit != null)
				compUnits.add(compUnit);
		}
	}

}
