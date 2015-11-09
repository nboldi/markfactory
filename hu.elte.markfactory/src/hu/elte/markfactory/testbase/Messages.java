package hu.elte.markfactory.testbase;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "hu.elte.markfactory.testbase.messages"; //$NON-NLS-1$
	public static String ReflectionTester_classNotFound;
	public static String ReflectionTester_constructorNotFound;
	public static String ReflectionTester_constructorNotVisible;

	public static String ReflectionTester_fieldNotFound;
	public static String ReflectionTester_fieldNotStatic;
	public static String ReflectionTester_fieldNotVisible;
	public static String ReflectionTester_methodFound;
	public static String ReflectionTester_methodNotFound;
	public static String ReflectionTester_methodNotStatic;
	public static String ReflectionTester_methodNotVisible;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
