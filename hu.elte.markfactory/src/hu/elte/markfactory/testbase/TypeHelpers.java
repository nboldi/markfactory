package hu.elte.markfactory.testbase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class TypeHelpers {

	public static boolean equalWithoutBoxing(Class<?> cl1, Class<?> cl2) {
		return boxType(cl1).equals(boxType(cl2));
	}

	private static Class<?> boxType(Class<?> unboxed) {
		if (unboxed.equals(boolean.class))
			return Boolean.class;
		if (unboxed.equals(byte.class))
			return Byte.class;
		if (unboxed.equals(char.class))
			return Character.class;
		if (unboxed.equals(double.class))
			return Double.class;
		if (unboxed.equals(float.class))
			return Float.class;
		if (unboxed.equals(int.class))
			return Integer.class;
		if (unboxed.equals(long.class))
			return Long.class;
		if (unboxed.equals(short.class))
			return Short.class;
		return unboxed;
	}

	public static String showType(Class<?>[] types) {
		StringBuilder sb = new StringBuilder();
		if (types.length > 0) {
			for (int i = 0; i < types.length - 1; i++) {
				sb.append(showType(types[i])).append(", ");
			}
			sb.append(showType(types[types.length - 1]));
		}
		return sb.toString();
	}

	public static String showType(Class<?> type) {
		if (type.isArray()) {
			return showType(type.getComponentType()) + "[]";
		} else {
			return type.getSimpleName();
		}
	}

	public static String decodeType(String coded) {
		switch (coded) {
		case "I":
			return "int";
		case "B":
			return "byte";
		case "C":
			return "char";
		case "F":
			return "float";
		case "D":
			return "double";
		case "J":
			return "long";
		case "S":
			return "short";
		case "V":
			return "void";
		case "Z":
			return "boolean";

		default:
			if (coded.charAt(0) == '[')
				return decodeType(coded.substring(1)) + "[]";
			if (coded.charAt(0) == 'L' || coded.charAt(0) == 'Q') {
				return decodeClassType(coded.substring(1, coded.length() - 1));
			}
			return coded;
		}
	}

	public static String encodeType(String typeName) {
		switch (typeName) {
		case "int":
			return "I";
		case "byte":
			return "B";
		case "char":
			return "C";
		case "float":
			return "F";
		case "double":
			return "D";
		case "long":
			return "J";
		case "short":
			return "S";
		case "void":
			return "V";
		case "boolean":
			return "Z";

		default:
			if (typeName.endsWith("[]")) {
				return "[" + encodeType2(typeName.substring(0, typeName.length() - 2));
			}
			return typeName;
		}
	}

	private static String encodeType2(String typeName) {
		switch (typeName) {
		case "int":
			return "I";
		case "byte":
			return "B";
		case "char":
			return "C";
		case "float":
			return "F";
		case "double":
			return "D";
		case "long":
			return "J";
		case "short":
			return "S";
		case "void":
			return "V";
		case "boolean":
			return "Z";

		default:
			if (typeName.endsWith("[]")) {
				return "[" + encodeType2(typeName.substring(0, typeName.length() - 2));
			}
			return "L" + typeName + ";";
		}
	}

	public static Class<?> parseType(String typeCode) throws ClassNotFoundException {
		switch (typeCode) {
		case "I":
			return Integer.TYPE;
		case "B":
			return Byte.TYPE;
		case "C":
			return Character.TYPE;
		case "F":
			return Float.TYPE;
		case "D":
			return Double.TYPE;
		case "J":
			return Long.TYPE;
		case "S":
			return Short.TYPE;
		case "V":
			return Void.TYPE;
		case "Z":
			return Boolean.TYPE;

		default:
			return Class.forName(typeCode);
		}
	}

	public static String decodeClassType(String coded) {
		if (coded.contains("<") && coded.contains(">")) {
			int genericStart = coded.indexOf('<');
			return coded.substring(0, genericStart);
		}
		return coded;
	}

	public static String methodSignature(Method method) {
		return method.getName() + "(" + showType(method.getParameterTypes()) + ")";
	}

	public static String ctorSignature(Constructor<?> method) {
		return method.getDeclaringClass().getSimpleName() + "(" + showType(method.getParameterTypes()) + ")";
	}

	public static Class<?>[] getTypes(Object... parameters) {
		if (parameters == null) {
			return new Class<?>[] { null };
		}
		Class<?>[] ptypes = new Class<?>[parameters.length];
		for (int i = 0; i < parameters.length; ++i) {
			if (parameters[i] == null) {
				ptypes[i] = null;
			} else {
				ptypes[i] = parameters[i].getClass();
			}
		}
		return ptypes;
	}

}
