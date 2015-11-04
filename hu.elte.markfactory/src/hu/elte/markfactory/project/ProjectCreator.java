package hu.elte.markfactory.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
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
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			project.build(IncrementalProjectBuilder.FULL_BUILD, null);
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

	/**
	 * Adds the Java nature if not already present
	 */
	private static void setNature(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] natureIds = description.getNatureIds();
		if (!Arrays.asList(natureIds).contains(JavaCore.NATURE_ID)) {
			String[] newNatureIds = Arrays.copyOf(natureIds, natureIds.length + 1);
			newNatureIds[natureIds.length] = JavaCore.NATURE_ID;
			description.setNatureIds(newNatureIds);
			project.setDescription(description, null);
		}
	}

	private static void createBinFolder(IProject project) throws CoreException, JavaModelException {
		IFolder binFolder = createFolder(project, "bin");
		JavaCore.create(project).setOutputLocation(binFolder.getFullPath(), null);
	}

	/**
	 * Creates the new folder if not already present
	 */
	private static IFolder createFolder(IProject project, String name) {
		IFolder folder = project.getFolder(name);
		try {
			if (!folder.exists()) {
				folder.create(false, true, null);
			}
		} catch (CoreException e) {
			MarkfactoryPlugin.logError("Error while creating folder.", e); //$NON-NLS-1$
		}
		return folder;
	}

	/**
	 * Adds the JRE runtime, the markfactory library and the source folder to
	 * the java classpath.
	 */
	private static void setupClassPath(IJavaProject javaProject, String sourceFolder) {
		try {
			List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(
					Arrays.asList(javaProject.getRawClasspath()));
			entries.remove(JavaCore.newSourceEntry(javaProject.getPath()));
			// the root should not be a source directory
			IClasspathEntry sourceEntry = JavaCore.newSourceEntry(javaProject.getPath().append(sourceFolder));
			if (!entries.contains(sourceEntry)) {
				entries.add(sourceEntry);
			}
			IClasspathEntry jreEntry = JavaCore.newContainerEntry(JRE_CONTAINER_PATH);
			if (!entries.contains(jreEntry)) {
				entries.add(jreEntry);
			}
			IClasspathEntry containerEntry = JavaCore
					.newContainerEntry(RuntimeLibraryContainerInitializer.LIBRARY_PATH);
			if (!entries.contains(containerEntry)) {
				entries.add(containerEntry);
			}
			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
		} catch (JavaModelException e) {
			MarkfactoryPlugin.logError("Cannot setup class path", e); //$NON-NLS-1$
		}
	}

}
