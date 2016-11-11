package com.alibaba.dubbo.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ProxyFactory {

	private static final String classBoundSymbol = ".";

	private static Map<String, Class<?>> classPool = new LinkedHashMap<String, Class<?>>(100);

	private static Map<String, Method> methodPool = new LinkedHashMap<String, Method>(1000);

	private static Map<String, ProxyConfig> configPool = new LinkedHashMap<String, ProxyConfig>(1000);

	private static Map<String, Object> proxyPool = new LinkedHashMap<String, Object>();

	private static final ReadWriteLock currentLock = new ReentrantReadWriteLock(true);

	private static void registry(String packageName, String interfaceName, String methodName) {
		String interfaceKey = packageName + classBoundSymbol + interfaceName;
		if (!classPool.containsKey(interfaceKey)) {
			registryInterface(packageName, interfaceName, methodName);
		}

		String methodKey = interfaceKey + classBoundSymbol + methodName;
		if (!methodPool.containsKey(methodKey)) {
			registryMethod(classPool.get(interfaceKey), methodName);
		}
	}

	public static void registry(Properties properties) {
		String interfaceClass = properties.getProperty("dubbo.reference.interface");
		String packageName = interfaceClass.substring(0, interfaceClass.lastIndexOf("."));
		String interfaceName = interfaceClass.substring(interfaceClass.lastIndexOf(".") + 1);
		String methodName = properties.getProperty("dubbo.reference." + interfaceClass + ".method.name");
		registry(packageName, interfaceName, methodName);
		String key = packageName + classBoundSymbol + interfaceName + classBoundSymbol + methodName;
		if (!configPool.containsKey(key)) {
			registryConfig(packageName, interfaceName, methodName, properties);
		}
		if (!proxyPool.containsKey(interfaceClass)) {
			registryProxy(packageName, interfaceName, methodName);
		}
	}

	private static void registryProxy(String packageName, String interfaceName, String methodName) {
		String interfaceKey = packageName + classBoundSymbol + interfaceName;
		Class<?> classType = classPool.get(interfaceKey);
		String methodKey = interfaceKey + classBoundSymbol + methodName;
		ProxyConfig proxyConfig = configPool.get(methodKey);
		try {
			currentLock.writeLock().lock();
			Object object = proxyConfig.get(classType);
			proxyPool.put(interfaceKey, object);
		} finally {
			currentLock.writeLock().unlock();
		}
	}

	private static void registryConfig(String packageName, String interfaceName, String methodName,
			Properties properties) {
		try {
			currentLock.writeLock().lock();
			ProxyConfig config = new ProxyConfig().build(properties);
			String key = packageName + classBoundSymbol + interfaceName + classBoundSymbol + methodName;
			configPool.put(key, config);
		} finally {
			currentLock.writeLock().unlock();
		}
	}

	private static void registryInterface(String packageName, String interfaceName, String methodName) {
		StringBuffer javaCode = JavaCodeFactory.createJavaCode(packageName, interfaceName, methodName);
		String className = packageName + classBoundSymbol + interfaceName;
		String methodKey = className + classBoundSymbol + methodName;
		try {
			currentLock.writeLock().lock();
			Class<?> classType = JavaCodeCompile.compile(javaCode.toString(), className);
			classPool.put(className, classType);
			methodPool.put(methodKey, classType.getMethod(methodName, String.class));
		} catch (ClassNotFoundException | MalformedURLException | NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("interface " + className + " registry fail !!", e);
		} finally {
			currentLock.writeLock().unlock();
		}
	}

	private static void registryMethod(Class<?> interfaceClass, String methodName) {
		String className = interfaceClass.getCanonicalName();
		List<String> methodNames = new ArrayList<String>();
		for (Method method : interfaceClass.getMethods()) {
			methodNames.add(method.getName());
			if (method.getName().equals(methodName)) {
				return;
			}
		}
		methodNames.add(methodName);
		StringBuffer javaCode = JavaCodeFactory.createJavaCode(interfaceClass.getPackage().getName(),
				interfaceClass.getName(), methodNames);
		try {
			currentLock.writeLock().lock();
			Class<?> classType = JavaCodeCompile.compile(javaCode.toString(), className);
			classPool.put(className, classType);
			methodPool.put(className + classBoundSymbol + methodName, classType.getMethod(methodName, String.class));
		} catch (ClassNotFoundException | MalformedURLException | NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("interface " + className + " registry fail !!", e);
		} finally {
			currentLock.writeLock().unlock();
		}
	}

	public static Object proxy(String interfaceName, String methodName, Object... args) {
		if (!classPool.containsKey(interfaceName))
			throw new IllegalStateException("interface " + interfaceName + " not registry !!");
		String methodKey = interfaceName + classBoundSymbol + methodName;
		if (!configPool.containsKey(methodKey))
			throw new IllegalStateException("method " + methodKey + " not registry configure informations");
		Object object = proxyPool.get(interfaceName);
		Method method = methodPool.get(methodKey);
		try {
			return method.invoke(object, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalStateException(
					"inteface " + interfaceName + " method " + methodName + " args " + args + " failed !!");
		}
	}

}
