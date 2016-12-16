package com.alibaba.dubbo.demo.consumer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.rpc.RpcContext;

public class DemoConsumerTest {
	public static void main(String[] args) {

		// 当前应用配置
		ApplicationConfig application = new ApplicationConfig();
		application.setName("demo-consumer");

		// 连接注册中心配置
		RegistryConfig registry = new RegistryConfig();
		registry.setAddress("c01.zk.dtc.uat:2181");
		registry.setProtocol("zookeeper");
		registry.setGroup("test");

		// 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接

		// 引用远程服务
		ReferenceConfig reference = new ReferenceConfig(); // 此实例很重，封装了与注册中心的连接以及与提供者的连接，请自行缓存，否则可能造成内存和连接泄漏
		reference.setApplication(application);
		reference.setRegistry(registry); // 多个注册中心可以用setRegistries()
		reference.setInterface(DemoService.class);
//		reference.setVersion("1.0.0");

		// 和本地bean一样使用xxxService
		String eid = RpcContext.getContext().getHeader(Constants.GENERIC_HEADER_EID);
	//	DemoService demoService = reference.get(); // 注意：此代理对象内部封装了所有通讯细节，对象较重，请缓存复用
		Class classType = DemoService.class;
		try {
			Method method = classType.getMethod("sayHello", String.class);
			method.invoke(reference.get(), "hello world");
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
