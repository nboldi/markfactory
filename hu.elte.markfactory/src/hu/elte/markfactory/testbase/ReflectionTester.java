package hu.elte.markfactory.testbase;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ReflectionTester {

	private static Set<String> errorsShown = new HashSet<>();

	public static Object construct(String className, String[] paramTypes, Object[] parameters) {
		return construct(className, toClasses(paramTypes), parameters);
	}

	/**
	 * Creates an object using an accessible constructor, by specifying the
	 * types of the arguments. Should only be used when there are constructors
	 * that differ only in the boxedness of their arguments.
	 */
	public static Object construct(String className, Class<?>[] paramTypes, Object[] parameters) {
		Constructor<?> c = loadConstructor(className, paramTypes);
		try {
			if ((c.getModifiers() & Modifier.PUBLIC) == 0) {
				System.err.println(String.format(Messages.ReflectionTester_constructorNotVisible, c.getName(),
						TypeHelpers.showType(paramTypes)));
			}
			c.setAccessible(true);
			return c.newInstance(parameters);

		} catch (InvocationTargetException e) {
			Throwable cause = removeSystemLines(e.getCause());
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object call(String methodName, Object instance, String[] paramTypes, Object[] parameters) {
		return call(methodName, instance, toClasses(paramTypes), parameters);
	}

	public static Object call(String methodName, Object instance, Class<?>[] paramTypes, Object[] parameters) {
		Method m = loadMethod(instance.getClass(), methodName, paramTypes);
		try {
			return m.invoke(instance, parameters);
		} catch (InvocationTargetException e) {
			Throwable cause = removeSystemLines(e.getCause());
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	private static Throwable removeSystemLines(Throwable cause) {
		StackTraceElement[] st = cause.getStackTrace();
		LinkedList<StackTraceElement> stackTrace = new LinkedList<>(Arrays.asList(st));
		// stackTrace.removeIf(ReflectionTester::notTestSolution);
		cause.setStackTrace(stackTrace.toArray(new StackTraceElement[stackTrace.size()]));
		return cause;
	}

	// private static boolean notTestSolution(StackTraceElement ste) {
	// try {
	// Class<?> cls = Class.forName(ste.getClassName());
	// return cls.getDeclaredAnnotation(TestSolution.class) == null
	// && cls.getDeclaredAnnotation(ExamTest.class) == null;
	// } catch (ClassNotFoundException e) {
	// return false;
	// }
	// }

	public static Object staticCall(String className, String method, String[] paramTypes, Object[] parameters) {
		return staticCall(className, method, toClasses(paramTypes), parameters);
	}

	public static Object staticCall(String className, String method, Class<?>[] paramTypes, Object[] parameters) {
		Method m = loadStaticMethod(className, method, paramTypes);
		try {
			return m.invoke(null, parameters);
		} catch (InvocationTargetException e) {
			Throwable cause = removeSystemLines(e.getCause());
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else {
				throw new RuntimeException(cause);
			}
		} catch (IllegalAccessException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	private static Class<?>[] toClasses(String[] paramTypes) {
		Class<?>[] newParamTypes = new Class<?>[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			newParamTypes[i] = loadClass(paramTypes[i]);
		}
		return newParamTypes;
	}

	public static Object fieldValue(Object obj, String fieldName) {
		Field f = loadField(obj.getClass(), fieldName);
		try {
			return f.get(obj);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object staticFieldValue(String className, String fieldName) {
		Field f = loadStaticField(className, fieldName);
		try {
			return f.get(null);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object fieldSet(Object obj, String fieldName, Object newValue) {
		Field f = loadField(obj.getClass(), fieldName);
		try {
			f.set(obj, newValue);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return newValue;
	}

	public static Object fieldSetReturnOld(Object obj, String fieldName, Object newValue) {
		Object oldVal = fieldValue(obj, fieldName);
		Field f = loadField(obj.getClass(), fieldName);
		try {
			f.set(obj, newValue);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return oldVal;
	}

	public static Object staticFieldSet(String className, String fieldName, Object newValue) {
		Field f = loadStaticField(className, fieldName);
		try {
			f.set(null, newValue);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			new RuntimeException(e);
		}
		return newValue;
	}

	public static Object staticFieldSetReturnOld(String className, String fieldName, Object newValue) {
		Object oldValue = staticFieldValue(className, fieldName);
		Field f = loadStaticField(className, fieldName);
		try {
			f.set(null, newValue);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		return oldValue;
	}

	public static Object createArray(String clsName, Object[] elements) {
		return createArray(loadClass(clsName), elements);
	}

	private static Object createArray(Class<?> cls, Object[] elements) {
		Object newArray = Array.newInstance(cls, elements.length);
		for (int i = 0; i < elements.length; i++) {
			Array.set(newArray, i, elements[i]);
		}
		return newArray;
	}

	public static Constructor<?> loadConstructor(String className, Class<?>... arguments) {
		Class<?> cl = loadClass(className);
		for (Constructor<?> ctr : cl.getDeclaredConstructors()) {
			Class<?>[] paramTypes = ctr.getParameterTypes();
			if (callable(paramTypes, arguments)) {
				return ctr;
			}
		}
		throw new MissingProgramElementException(String.format(Messages.ReflectionTester_constructorNotFound, className,
				TypeHelpers.showType(arguments)));
	}

	private static boolean callable(Class<?>[] formalParams, Class<?>[] actualParams) {
		if (formalParams.length == actualParams.length) {
			for (int i = 0; i < formalParams.length; ++i) {
				if (actualParams[i] != null && !formalParams[i].isAssignableFrom(actualParams[i])
						&& !TypeHelpers.equalWithoutBoxing(formalParams[i], actualParams[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public static Method loadMethod(Class<?> methodClass, String methodName, Class<?>[] arguments) {

		StringBuilder additionalInfos = new StringBuilder();
		List<Method> methods = new LinkedList<>(Arrays.asList(methodClass.getDeclaredMethods()));
		methods.addAll(Arrays.asList(methodClass.getMethods()));
		for (Method method : methods) {
			if (method.getName().equals(methodName)) {
				if (callable(method.getParameterTypes(), arguments)) {
					if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
						System.err.println(String.format(Messages.ReflectionTester_methodNotVisible, methodName));
					}
					method.setAccessible(true);
					return method;
				} else {
					String message = String.format(Messages.ReflectionTester_methodFound, method.getName(),
							TypeHelpers.showType(method.getParameterTypes()));
					additionalInfos.append(message);
				}
			}
		}
		throw new MissingProgramElementException(String.format(Messages.ReflectionTester_methodNotFound, methodName,
				TypeHelpers.showType(arguments), additionalInfos.toString()));
	}

	public static Method loadStaticMethod(String className, String methodName, Class<?>[] arguments) {
		Class<?> cl = loadClass(className);
		Method result = loadMethod(cl, methodName, arguments);
		if ((result.getModifiers() & Modifier.PUBLIC) == 0) {
			System.err.println(String.format(Messages.ReflectionTester_methodNotVisible, methodName));
		}
		result.setAccessible(true);
		if (Modifier.isStatic(result.getModifiers())) {
			return result;
		} else {
			throw new MissingProgramElementException(
					String.format(Messages.ReflectionTester_methodNotStatic, methodName));
		}
	}

	public static Field loadField(Class<?> fieldClass, String fieldName) {
		try {
			Field field = fieldClass.getDeclaredField(fieldName);
			if ((field.getModifiers() & Modifier.PUBLIC) == 0) {
				System.err.println(String.format(Messages.ReflectionTester_fieldNotVisible, fieldName));
			}
			field.setAccessible(true);
			return field;

		} catch (Exception e) {
			throw new MissingProgramElementException(String.format(Messages.ReflectionTester_fieldNotFound, fieldName));
		}
	}

	public static Field loadStaticField(String className, String fieldName) {
		Class<?> cl = loadClass(className);
		Field result = loadField(cl, fieldName);
		if ((result.getModifiers() & Modifier.PUBLIC) == 0) {
			System.err.println(String.format(Messages.ReflectionTester_fieldNotVisible, fieldName));
		}
		result.setAccessible(true);
		if (Modifier.isStatic(result.getModifiers())) {
			return result;
		} else {
			throw new MissingProgramElementException(
					String.format(Messages.ReflectionTester_fieldNotStatic, fieldName));
		}
	}

	public static Class<?> loadClass(String className) {
		try {
			return TypeHelpers.parseType(TypeHelpers.encodeType(className));
		} catch (Exception e) {
			throw new MissingProgramElementException(String.format(Messages.ReflectionTester_classNotFound, className));
		}
	}

	protected static void output(Exception e) {
		if (!errorsShown.contains(e.getMessage())) {
			errorsShown.add(e.getMessage());
			System.err.println(e.getMessage());
		}
	}

	protected static <T> void skip(T inp) {
	}

}
