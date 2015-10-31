package hu.elte.markfactory;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "hu.elte.markfactory.messages"; //$NON-NLS-1$
	public static String RuntimeLibraryContainerWizardPage_wizard_description;
	public static String RuntimeLibraryContainerWizardPage_wizard_text;
	public static String RuntimeLibraryContainerWizardPage_wizard_title;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
