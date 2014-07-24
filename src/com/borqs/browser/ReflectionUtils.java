/**
 *  Created by ChaoMeng.
 */

package com.borqs.browser;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectionUtils {

	public static Object invokeMethod(Object owner, String methodName, Object[] args) throws Exception {
		if (owner == null) {
			return null;
		}
		Class ownerClass = owner.getClass();
		if (ownerClass == null) {
			return null;
		}

		Class[] argsClass = {};
		if (args != null && args.length > 0) {
			argsClass = new Class[args.length];
			for (int i = 0, j = args.length; i < j; i++) {
				argsClass[i] = args[i].getClass();
			}
		}

		Method method = null;
		try {
			// method = ownerClass.getDeclaredMethod(methodName, argsClass);
			method = ownerClass.getMethod(methodName, argsClass);
		} catch (NoSuchMethodException e) {
			Method[] methods= ownerClass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methodName.equals(methods[i].getName())) {
					method = methods[i];
					break;
				}
			}
		}

		if (method == null) {
			return null;
		}
		return method.invoke(owner, args);
	}
/*
	public static Object invokeCastMethod(Object owner, String methodName, Object[] args) throws Exception {
		if (owner == null) {
			return null;
		}
		Class ownerClass = owner.getClass();
		if (ownerClass == null) {
			return null;
		}

		Class[] argsClass = {};
		if (args != null && args.length > 0) {
			argsClass = new Class[args.length];
			for (int i = 0, j = args.length; i < j; i++) {
				argsClass[i] = args[i].getClass();
			}
		}

		Method method = null;
		try {
			// method = ownerClass.getDeclaredMethod(methodName, argsClass);
			method = ownerClass.getMethod(methodName, argsClass);
		} catch (NoSuchMethodException e) {
			Method[] methods= ownerClass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methodName.equals(methods[i].getName())) {
					method = methods[i];
					break;
				}
			}
		}

		if (method == null) {
			return null;
		}
		
		return method.invoke(owner, args);
	}
*/
	public static Object invokeStaticMethod(String className, String methodName, Object[] args) throws Exception {
		Class ownerClass = Class.forName(className);
		if (ownerClass == null) {
			return null;
		}

		Class[] argsClass = {};
		if (args != null && args.length > 0) {
			argsClass = new Class[args.length];
			for (int i = 0, j = args.length; i < j; i++) {
				argsClass[i] = args[i].getClass();
			}
		}
		
		Method method = null;
		try {
			// method = ownerClass.getDeclaredMethod(methodName, argsClass);
			method = ownerClass.getMethod(methodName, argsClass);
		} catch (NoSuchMethodException e) {
			Method[] methods= ownerClass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				if (methodName.equals(methods[i].getName())) {
					method = methods[i];
					break;
				}
			}
		}

		if (method == null) {
			return null;
		}
		return method.invoke(null, args);
	}

	public static Object getProperty(Object owner, String fieldName) throws Exception {
		Class ownerClass = owner.getClass();
		if (ownerClass == null) {
			return null;
		}
		Field field = ownerClass.getField(fieldName);
		Object property = field.get(owner);
		return property;
	}

	public static Object getStaticProperty(String className, String fieldName) throws Exception {
		Class ownerClass = Class.forName(className);
		if (ownerClass == null) {
			return null;
		}
		Field field = ownerClass.getField(fieldName);
		Object property = field.get(ownerClass);
		return property;
	}

	public static boolean isInstance(Object obj, Class cls) {
		if (obj == null) {
			return false;
		}
		return cls.isInstance(obj);
	}

	public static Object newInstance(String className, Object[] args) throws Exception {
		Class newoneClass = Class.forName(className);
		if (newoneClass == null) {
			return null;
		}
		Class[] argsClass = new Class[args.length];
		for (int i = 0, j = args.length; i < j; i++) {
			argsClass[i] = args[i].getClass();
		}
		Constructor cons = newoneClass.getConstructor(argsClass);
		return cons.newInstance(args);
	} 

	public static Object getByArray(Object array, int index) {
		return Array.get(array,index);
	}

	public static Object cast(Object obj, String newType) throws Exception {
		Class ownerClass = Class.forName(newType);
		if (ownerClass == null) {
			return null;
		}
		return ownerClass.cast(obj);
	}
}
