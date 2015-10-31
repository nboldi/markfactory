package hu.elte.markfactory;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle. Enables logging to other
 * classes.
 */
public class MarkfactoryPlugin extends AbstractUIPlugin implements IStartup {

	// The plug-in ID
	public static final String PLUGIN_ID = "hu.elte.markfactory";

	// The shared instance
	private static MarkfactoryPlugin plugin;

	/**
	 * The constructor
	 */
	public MarkfactoryPlugin() {
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MarkfactoryPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	private static ILog log = null;

	public static void log(IStatus status) {
		if (log != null) {
			log.log(status);
		}
	}

	public static void logError(String msg, Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, msg, e));
	}

	public static void logError(String msg) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, msg));
	}

	public static void logInfo(String msg) {
		log(new Status(IStatus.INFO, PLUGIN_ID, msg));
	}

	@Override
	public void earlyStartup() {
		log = Platform.getLog(Platform.getBundle(PLUGIN_ID));
	}

}
