package com.alibaba.dubbo.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;

public class ProxyConfig {
	private ApplicationConfig applicationConfig;
	private ProtocolConfig protocolConfig;
	private MethodConfig methodConfig;
	private MonitorConfig monitorConfig;
	private RegistryConfig registryConfig;
	private ConsumerConfig consumerConfig;
	private ReferenceConfig<?> referenceConfig;

	private boolean initialize;

	public ProxyConfig() {
	}

	@SuppressWarnings("rawtypes")
	private void initialize() {
		this.applicationConfig = new ApplicationConfig();
		this.protocolConfig = new ProtocolConfig();
		this.methodConfig = new MethodConfig();
		this.monitorConfig = new MonitorConfig();
		this.referenceConfig = new ReferenceConfig();
		this.registryConfig = new RegistryConfig();
		this.consumerConfig = new ConsumerConfig();
		referenceConfig.setApplication(applicationConfig);
		referenceConfig.setRegistry(registryConfig);
		List<MethodConfig> methods = new ArrayList<MethodConfig>();
		methods.add(methodConfig);
		referenceConfig.setMethods(methods);
	}

	public ProxyConfig build(Properties properties) {
		if (!initialize)
			initialize();
		build(this.applicationConfig, properties);
		build(this.protocolConfig, properties);
		build(this.methodConfig, properties);
		build(this.registryConfig, properties);
		build(this.monitorConfig, properties);
		build(this.referenceConfig, properties);
		build(this.consumerConfig, properties);
		return this;
	}

	private void build(Object config, Properties properties) {
		if (config == null)
			return;
		Method[] methods = config.getClass().getMethods();
		String prefix = getPrefix(config);
		for (Method method : methods) {
			try {
				String name = method.getName();
				if (name.startsWith("set") && Modifier.isPublic(method.getModifiers())
						&& method.getParameterTypes().length == 1 && isPrimitive(method.getReturnType())) {
					String property = "dubbo." + prefix + name;
					String value = properties.getProperty(property);
					Class<?> parameterType = method.getParameterTypes()[0];
					if (value != null) {
						Object obj = null;
						if (parameterType == String.class) {
							obj = value;
						} else if (parameterType == Integer.class) {
							obj = Integer.valueOf(value);
						} else if (parameterType == Short.class) {
							obj = Short.valueOf(value);
						} else if (parameterType == Double.class) {
							obj = Double.valueOf(value);
						} else if (parameterType == Float.class) {
							obj = Double.valueOf(value);
						} else if (parameterType == Long.class) {
							obj = Double.valueOf(value);
						} else if (parameterType == Byte.class) {
							obj = Byte.valueOf(value);
						}
						method.invoke(config, obj);
					}
				}
			} catch (Exception e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
	}

	private String getPrefix(Object config) {
		if (config instanceof ApplicationConfig)
			return "application";
		if (config instanceof MethodConfig)
			return "method";
		if (config instanceof ConsumerConfig)
			return "consumer";
		if (config instanceof ReferenceConfig)
			return "reference";
		if (config instanceof RegistryConfig)
			return "registry";
		if (config instanceof ProtocolConfig)
			return "protocol";
		if (config instanceof MonitorConfig)
			return "monitor";
		return "default";
	}

	private boolean isPrimitive(Class<?> type) {
		return type.isPrimitive() || type == Void.TYPE;
	}

	public Object get() {
		return referenceConfig.get();
	}

	public Object get(Class<?> classType) {
		this.referenceConfig.setInterface(classType);
		return referenceConfig.get();
	}
}
