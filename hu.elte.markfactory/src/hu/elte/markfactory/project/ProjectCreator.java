package hu.elte.markfactory.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.JavaRuntime;

import hu.elte.markfactory.MarkfactoryPlugin;
import hu.elte.markfactory.buildpath.RuntimeLibraryContainerInitializer;

public class ProjectCreator {

	public static final String JAVA_SE_VERSION = "JavaSE-1.8"; //$NON-NLS-1$

	public static final IPath JRE_CONTAINER_PATH = JavaRuntime.newDefaultJREContainerPath()
			.append(StandardVMType.ID_STANDARD_VM_TYPE).append(JAVA_SE_VERSION);

	public static IProject createOrUpdateProject(String projectName) {

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(projectName);
		try {
			createAndOpen(project);
			setNature(project);
			createBinFolder(project);
			setupClassPath(JavaCore.create(project), "src");
		} catch (CoreException e) {
			MarkfactoryPlugin.logError("Error while creating project", e);
		}

		return project;
	}

	private static void createAndOpen(IProject project) throws CoreException {
		if (!project.exists()) {
			project.create(null);
		}
		if (!project.isOpen()) {
			project.open(null);
		}
	}

	private static void setNature(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);
	}

	private static void createBinFolder(IProject project) throws CoreException, JavaModelException {
		IFolder binFolder = createFolder(project, "bin");
		JavaCore.create(project).setOutputLocation(binFolder.getFullPath(), null);
	}

	private static IFolder createFolder(IProject project, String name) {
		IFolder sourceFolder = project.getFolder(name);
		try {
			sourceFolder.create(false, true, null);
		} catch (CoreException e) {
			MarkfactoryPlugin.logError("Error while creating folder.", e); //$NON-NLS-1$
		}
		return sourceFolder;
	}

	private static void setupClassPath(IJavaProject javaProject, String sourceFolder) {
		try {
			List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
			entries.add(JavaCore.newSourceEntry(javaProject.getPath().append(sourceFolder)));
			IClasspathEntry jreEntry = JavaCore.newContainerEntry(JRE_CONTAINER_PATH);
			entries.add(jreEntry);
			IClasspathEntry containerEntry = JavaCore
					.newContainerEntry(RuntimeLibraryContainerInitializer.LIBRARY_PATH);
			entries.add(containerEntry);
			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
		} catch (JavaModelException e) {
			MarkfactoryPlugin.logError("Cannot setup class path", e); //$NON-NLS-1$
		}
	}

}
