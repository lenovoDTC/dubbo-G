package com.alibaba.dubbo.proxy.dynamic.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ProxyInvacationHandler implements InvocationHandler{

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("proxy test -------------------------------");
		return null;
	}

}
