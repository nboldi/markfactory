package hu.elte.markfactory.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import hu.elte.markfactory.MarkfactoryPlugin;
import hu.elte.markfactory.builders.ExamTestBuilder;
import hu.elte.markfactory.buildpath.RuntimeLibraryContainerInitializer;

import org.eclipse.ui.handlers.HandlerUtil;

public class AddBuilderHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(final ExecutionEvent event) {
		final IProject project = getProject(event);

		if (project != null) {
			try {
				addBuilder(project);
				addLibrary(project);

			} catch (final CoreException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private void addBuilder(final IProject project) throws CoreException {
		if (hasBuilder(project)) {
			// already enabled
			return;
		}

		// add builder to project properties
		IProjectDescription description = project.getDescription();
		final ICommand buildCommand = description.newCommand();
		buildCommand.setBuilderName(ExamTestBuilder.BUILDER_ID);

		final List<ICommand> commands = new ArrayList<ICommand>();
		commands.addAll(Arrays.asList(description.getBuildSpec()));
		commands.add(buildCommand);

		description.setBuildSpec(commands.toArray(new ICommand[commands.size()]));
		project.setDescription(description, null);
	}

	private void addLibrary(IProject project) {
		try {
			IJavaProject javaProject = JavaCore.create(project);
			IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
			IClasspathEntry newCpEntry = JavaCore.newContainerEntry(RuntimeLibraryContainerInitializer.LIBRARY_PATH);
			if (Arrays.asList(rawClasspath).contains(newCpEntry)) {
				// already added
				return;
			}
			IClasspathEntry[] newClasspath = Arrays.copyOf(rawClasspath, rawClasspath.length + 1);
			newClasspath[rawClasspath.length] = newCpEntry;
			javaProject.setRawClasspath(newClasspath, null);
		} catch (CoreException e) {
			MarkfactoryPlugin.logError("Error while setting up classpath", e);
		}
	}

	public static IProject getProject(final ExecutionEvent event) {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			final Object element = ((IStructuredSelection) selection).getFirstElement();

			return (IProject) Platform.getAdapterManager().getAdapter(element, IProject.class);
		}

		return null;
	}

	public static final boolean hasBuilder(final IProject project) {
		try {
			for (final ICommand buildSpec : project.getDescription().getBuildSpec()) {
				if (ExamTestBuilder.BUILDER_ID.equals(buildSpec.getBuilderName()))
					return true;
			}
		} catch (final CoreException e) {
		}

		return false;
	}

}
