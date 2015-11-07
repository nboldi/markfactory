package hu.elte.markfactory.builders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
import org.eclipse.text.edits.MalformedTreeException;

import hu.elte.markfactory.MarkfactoryPlugin;
import hu.elte.markfactory.annotations.ExamTest;
import hu.elte.markfactory.project.ProjectCreator;
import hu.elte.markfactory.rewrite.AutocheckVisitor;
import hu.elte.markfactory.rewrite.HandoutVisitor;
import hu.elte.markfactory.rewrite.ModificationRecordingVisitor;

public class ExamTestBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "hu.elte.markfactory.examtestbuilder";

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {

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
			if (hasTestClass(compUnit)) {
				processClass(compUnit);
			}
		}
		return null;
	}

	private boolean hasTestClass(ICompilationUnit compUnit) throws JavaModelException {
		for (IType type : compUnit.getAllTypes()) {
			if (checkAnnotation(type, ExamTest.class)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkAnnotation(IJavaElement elem, Class<?> annot) throws JavaModelException {
		if (!(elem instanceof IAnnotatable))
			return false;
		IAnnotation[] annotations = ((IAnnotatable) elem).getAnnotations();
		for (IAnnotation annotation : annotations) {
			if (annotation.getElementName().equals(annot.getSimpleName()))
				return true;
		}
		return false;
	}

	private void processClass(ICompilationUnit compUnit) throws JavaModelException {
		generateModifiedFile(compUnit, "-autotest", AutocheckVisitor::new);
		generateModifiedFile(compUnit, "-handout", HandoutVisitor::new);
	}

	private void generateModifiedFile(ICompilationUnit compUnit, String suffix,
			Function<AST, ModificationRecordingVisitor> createVisitor) {
		try {
			ASTParser parser = ASTParser.newParser(AST.JLS8);
			parser.setSource(compUnit);
			parser.setResolveBindings(true);
			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
			AST ast = astRoot.getAST();
			IProject project = compUnit.getResource().getProject();
			astRoot.recordModifications();

			Document document = new Document(compUnit.getSource());

			String newSource = createVisitor.apply(ast).runOn(astRoot, document);
						
			if (newSource != null) {
				writeOutResult(compUnit, newSource, project.getName() + suffix);
			}

		} catch (MalformedTreeException | BadLocationException | CoreException | IOException e) {
			MarkfactoryPlugin.logError("Code transformation failed", e);
		}
	}

	private void writeOutResult(ICompilationUnit compUnit, String newSource, String projectName)
			throws IOException, FileNotFoundException, CoreException {

		IProject autoProject = ProjectCreator.createOrUpdateProject(projectName);
		IPath path = compUnit.getResource().getProjectRelativePath();

		File outFile = autoProject.getLocation().append(path).toFile();
		if (!outFile.exists()) {
			outFile.getParentFile().mkdirs();
			outFile.createNewFile();
		}
		PrintWriter out = new PrintWriter(outFile, "UTF-8");
		out.println(newSource);
		out.close();

		autoProject.refreshLocal(IProject.DEPTH_INFINITE, null);
		autoProject.findMember(path).setDerived(true, null);
	}

	private void addResourceAsCompUnit(final List<ICompilationUnit> compUnits, IResource resource) {
		if (resource.isAccessible() && !resource.isDerived() && resource.getType() == IResource.FILE) {
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
