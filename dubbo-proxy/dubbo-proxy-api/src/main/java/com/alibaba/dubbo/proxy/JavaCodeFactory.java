package com.alibaba.dubbo.proxy;

import java.util.ArrayList;
import java.util.List;

public class JavaCodeFactory {
	public static final String packageKeyWord = "package";
	public static final String interfaceKeyword = "interface";
	public static final String publicKeyword = "public";
	public static final String blankSymbol = " ";
	public static final String endSymbol = ";";
	public static final String EOF = "\n";

	public static StringBuffer createJavaCode(String packageName, String interfaceName, String methodName) {
		List<String> methodNames = new ArrayList<String>();
		methodNames.add(methodName);
		return createJavaCode(packageName, interfaceName, methodNames);
	}

	public static StringBuffer createJavaCode(String packageName, String interfaceName, List<String> methodNames) {
		StringBuffer javaCode = new StringBuffer();
		javaCode.append(packageKeyWord + blankSymbol + packageName + endSymbol + EOF);
		javaCode.append(publicKeyword + blankSymbol + interfaceKeyword + blankSymbol + interfaceName + "{" + EOF);
		createJavaCodeMethods(javaCode, methodNames);
		javaCode.append("}");
		return javaCode;
	}

	private static void createJavaCodeMethods(StringBuffer javaCode, List<String> methodNames) {
		for (String methodName : methodNames) {
			createJavaCodeMethod(javaCode, methodName);
		}
	}

	private static void createJavaCodeMethod(StringBuffer javaCode, String methodName) {
		javaCode.append(blankSymbol + blankSymbol + publicKeyword + blankSymbol + "String" + blankSymbol + methodName
				+ "()" + endSymbol + EOF);
	}

	public static void main(String[] args) {
		List<String> methodNames = new ArrayList<String>();
		methodNames.add("sayHello");
		StringBuffer sb = JavaCodeFactory.createJavaCode("com.lenovo.api", "Hello", methodNames);
		System.out.println(sb.toString());
	}
}
