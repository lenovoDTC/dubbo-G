package com.alibaba.dubbo.proxy.dynamic.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProxyTest {
	public static void main(String[] args) {
//		InvocationHandler handler = new ProxyInvacationHandler();
//		Class proxyClass = Proxy.getProxyClass(IProxy.class.getClassLoader(), new Class[] { IProxy.class });
//		try {
//			IProxy proxy = (IProxy) proxyClass.getConstructor(new Class[] { InvocationHandler.class })
//					.newInstance(new Object[] { handler });
//			proxy.sayHello();
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		long time = 1478188801012L;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
		String str = df.format(new Date(1478188801012L));
		System.out.println(str);
		
	}
}
