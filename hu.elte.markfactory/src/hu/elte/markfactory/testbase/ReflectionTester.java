package hu.elte.markfactory.testbase;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ReflectionTester {

	public static Object construct(String className, String[] paramTypes,
			Object[] parameters) throws Throwable {
		return construct(className, toClasses(paramTypes), parameters);
	}

	/**
	 * Creates an object using an accessible constructor, by specifying the
	 * types of the arguments. Should only be used when there are constructors
	 * that differ only in the boxedness of their arguments.
	 */
	public static Object construct(String className, Class<?>[] paramTypes,
			Object[] parameters) throws Throwable {
		Constructor<?> c = loadConstructor(className, paramTypes);
		try {
			return c.newInstance(parameters);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	public static Object call(String methodName, Object instance,
			String[] paramTypes, Object[] parameters) throws Throwable {
		return call(methodName, instance, toClasses(paramTypes), parameters);
	}

	public static Object call(String methodName, Object instance,
			Class<?>[] paramTypes, Object[] parameters) throws Throwable {
		Method m = loadMethod(instance.getClass(), methodName, paramTypes);
		try {
			return m.invoke(instance, parameters);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	public static Object staticCall(String className, String method,
			String[] paramTypes, Object[] parameters) throws Throwable {
		return staticCall(className, method, toClasses(paramTypes), parameters);
	}

	public static Object staticCall(String className, String method,
			Class<?>[] paramTypes, Object[] parameters) throws Throwable {
		Method m = loadStaticMethod(className, method, paramTypes);
		try {
			return m.invoke(null, parameters);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private static Class<?>[] toClasses(String[] paramTypes) throws Exception {
		Class<?>[] newParamTypes = new Class<?>[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			newParamTypes[i] = loadClass(paramTypes[i]);
		}
		return newParamTypes;
	}

	public static Object fieldValue(Object obj, String fieldName)
			throws Exception {
		Field f = loadField(obj.getClass(), fieldName);
		return f.get(obj);
	}

	public static Object staticFieldValue(String className, String fieldName)
			throws Exception {
		Field f = loadStaticField(className, fieldName);
		return f.get(null);
	}
	
	public static void fieldSet(Object obj, String fieldName, Object newValue)
			throws Exception {
		Field f = loadField(obj.getClass(), fieldName);
		f.set(obj, newValue);
	}
	
	public static void staticFieldSet(String className, String fieldName, Object newValue)
			throws Exception {
		Field f = loadStaticField(className, fieldName);
		f.set(null, newValue);
	}

	public static Object createArray(String clsName, Object[] elements)
			throws Exception {
		return createArray(loadClass(clsName), elements);
	}

	private static Object createArray(Class<?> cls, Object[] elements)
			throws Exception {
		Object newArray = Array.newInstance(cls, elements.length);
		for (int i = 0; i < elements.length; i++) {
			Array.set(newArray, i, elements[i]);
		}
		return newArray;
	}

	public static Constructor<?> loadConstructor(String className,
			Class<?>... arguments) throws Exception {
		Class<?> cl = loadClass(className);
		for (Constructor<?> ctr : cl.getDeclaredConstructors()) {
			Class<?>[] paramTypes = ctr.getParameterTypes();
			if (callable(paramTypes, arguments)) {
				return ctr;
			}
		}
		throw new MissingProgramElementException(
				String.format(
						"Nem talalhato a(z) %s osztaly konstruktora, amely a %s parametereket fogadja el.",
						className, Arrays.toString(arguments)));
	}

	private static boolean callable(Class<?>[] formalParams,
			Class<?>[] actualParams) {
		if (formalParams.length == actualParams.length) {
			for (int i = 0; i < formalParams.length; ++i) {
				if (actualParams[i] != null
						&& !formalParams[i].isAssignableFrom(actualParams[i])
						&& !TypeHelpers.equalWithoutBoxing(formalParams[i],
								actualParams[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static Method loadMethod(Class<?> methodClass, String methodName,
			Class<?>[] arguments) throws Exception {

		StringBuilder additionalInfos = new StringBuilder();
		List<Method> methods = new LinkedList<>(Arrays.asList(methodClass
				.getDeclaredMethods()));
		methods.addAll(Arrays.asList(methodClass.getMethods()));
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				if (callable(method.getParameterTypes(), arguments)) {
					return method;
				} else {
					String message = String.format(
							"Letezik %s nevu metodus (%s) parameterezessel.\n",
							method.getName(),
							TypeHelpers.showType(method.getParameterTypes()));
					additionalInfos.append(message);
				}
			}
		}
		throw new MissingProgramElementException(String.format(
				"Nem erheto el a(z) %s(%s) metodus. %s", methodName,
				TypeHelpers.showType(arguments), additionalInfos.toString()));
	}

	public static Method loadStaticMethod(String className, String methodName,
			Class<?>[] arguments) throws Exception {
		Class<?> cl = loadClass(className);
		Method result = loadMethod(cl, methodName, arguments);
		if (Modifier.isStatic(result.getModifiers())) {
			return result;
		} else {
			throw new MissingProgramElementException(String.format(
					"A(z) %s metodus nem statikus.", methodName));
		}
	}

	public static Field loadField(Class<?> fieldClass, String fieldName)
			throws Exception {
		try {
			return fieldClass.getDeclaredField(fieldName);
		} catch (Exception e) {
			throw new MissingProgramElementException(String.format(
					"Nem erheto el a(z) %s adattag.", fieldName));
		}
	}

	public static Field loadStaticField(String className, String fieldName)
			throws Exception {
		Class<?> cl = loadClass(className);
		Field result = loadField(cl, fieldName);
		if (Modifier.isStatic(result.getModifiers())) {
			return result;
		} else {
			throw new MissingProgramElementException(String.format(
					"A(z) %s adattag nem statikus.", fieldName));
		}
	}

	public static Class<?> loadClass(String className) throws Exception {
		try {
			return TypeHelpers.parseType(TypeHelpers.encodeType(className));
		} catch (Exception e) {
			throw new MissingProgramElementException(String.format(
					"Nem toltheto be a(z) %s osztaly.", className));
		}
	}
	
	protected static void output(Exception e) {
		System.err.println(e.getMessage());
	}

	protected static <T> void skip(T inp) {
	}

	public static void checkAPI(String className, String[] expectedInterface)
			throws Exception {
		try {
			Class<?> cl = loadClass(className);
			List<String> actualInterface = new ArrayList<String>();

			for (Method method : cl.getDeclaredMethods()) {
				if ((method.getModifiers() & Modifier.PUBLIC) > 0
						&& !method.isSynthetic() && !method.isBridge()) {
					actualInterface.add(TypeHelpers.methodSignature(method));
				}
			}
			for (Constructor<?> ctor : cl.getDeclaredConstructors()) {
				if ((ctor.getModifiers() & Modifier.PUBLIC) > 0
						&& !ctor.isSynthetic()) {
					actualInterface.add(TypeHelpers.ctorSignature(ctor));
				}
			}
			for (Field field : cl.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.PUBLIC) > 0
						&& !field.isSynthetic()) {
					actualInterface.add(field.getName());
				}
			}

			actualInterface.removeAll(Arrays.asList(expectedInterface));

			if (!actualInterface.isEmpty()) {
				throw new UnsupportedOperationException(
						String.format(
								"A(z) %s osztaly olyan publikus metodusokat vagy adattagokat tartalmaz, amelyet nem kert a feladat: %s!",
								className, actualInterface.toString()));
			}
		} catch (ClassNotFoundException e) {
			// ok, if class is not defined there can be no API problems
		}
	}
}
