/*
 * Copyright 1999-2012 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.spring;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.config.annotation.Parameter;
import com.alibaba.dubbo.config.annotation.Request;
import com.alibaba.dubbo.remoting.http.Mapping;
import com.alibaba.dubbo.remoting.http.ParameterMeta;
import com.alibaba.dubbo.remoting.http.RequestMeta;
import com.alibaba.dubbo.remoting.http.Schema;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.config.AbstractConfig;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ModuleConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;

/**
 * AnnotationBean
 *
 * @author william.liangf
 * @export
 */
public class AnnotationBean extends AbstractConfig implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {

    private static final long serialVersionUID = -7582802454287589552L;

    private static final Logger logger = LoggerFactory.getLogger(Logger.class);

    private String annotationPackage;

    private String[] annotationPackages;

    private final Set<ServiceConfig<?>> serviceConfigs = new ConcurrentHashSet<ServiceConfig<?>>();

    private final ConcurrentMap<String, ReferenceBean<?>> referenceConfigs = new ConcurrentHashMap<String, ReferenceBean<?>>();

    public String getPackage() {
        return annotationPackage;
    }

    public void setPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = (annotationPackage == null || annotationPackage.length() == 0) ? null
                : Constants.COMMA_SPLIT_PATTERN.split(annotationPackage);
    }

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        if (annotationPackage == null || annotationPackage.length() == 0) {
            return;
        }
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ReflectUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner");
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class}).newInstance(new Object[]{(BeanDefinitionRegistry) beanFactory, true});
                // add filter
                Class<?> filterClass = ReflectUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter");
                Object filter = filterClass.getConstructor(Class.class).newInstance(Service.class);
                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter", ReflectUtils.forName("org.springframework.core.type.filter.TypeFilter"));
                addIncludeFilter.invoke(scanner, filter);
                // scan packages
                String[] packages = Constants.COMMA_SPLIT_PATTERN.split(annotationPackage);
                Method scan = scannerClass.getMethod("scan", new Class<?>[]{String[].class});
                scan.invoke(scanner, new Object[]{packages});
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }

    public void destroy() throws Exception {
        for (ServiceConfig<?> serviceConfig : serviceConfigs) {
            try {
                serviceConfig.unexport();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
        for (ReferenceConfig<?> referenceConfig : referenceConfigs.values()) {
            try {
                referenceConfig.destroy();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Service service = bean.getClass().getAnnotation(Service.class);
        if (service != null) {
            ServiceBean<Object> serviceConfig = new ServiceBean<Object>(service);
            if (void.class.equals(service.interfaceClass())
                    && "".equals(service.interfaceName())) {
                if (bean.getClass().getInterfaces().length > 0) {
                    serviceConfig.setInterface(bean.getClass().getInterfaces()[0]);
                } else {
                    throw new IllegalStateException("Failed to export remote service class " + bean.getClass().getName() + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
                }
            }
            if (applicationContext != null) {
                serviceConfig.setApplicationContext(applicationContext);
                if (service.registry() != null && service.registry().length > 0) {
                    List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                    for (String registryId : service.registry()) {
                        if (registryId != null && registryId.length() > 0) {
                            registryConfigs.add((RegistryConfig) applicationContext.getBean(registryId, RegistryConfig.class));
                        }
                    }
                    serviceConfig.setRegistries(registryConfigs);
                }
                if (service.provider() != null && service.provider().length() > 0) {
                    serviceConfig.setProvider((ProviderConfig) applicationContext.getBean(service.provider(), ProviderConfig.class));
                }
                if (service.monitor() != null && service.monitor().length() > 0) {
                    serviceConfig.setMonitor((MonitorConfig) applicationContext.getBean(service.monitor(), MonitorConfig.class));
                }
                if (service.application() != null && service.application().length() > 0) {
                    serviceConfig.setApplication((ApplicationConfig) applicationContext.getBean(service.application(), ApplicationConfig.class));
                }
                if (service.module() != null && service.module().length() > 0) {
                    serviceConfig.setModule((ModuleConfig) applicationContext.getBean(service.module(), ModuleConfig.class));
                }
                if (service.provider() != null && service.provider().length() > 0) {
                    serviceConfig.setProvider((ProviderConfig) applicationContext.getBean(service.provider(), ProviderConfig.class));
                } else {

                }
                if (service.protocol() != null && service.protocol().length > 0) {
                    List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
                    for (String protocolId : service.registry()) {
                        if (protocolId != null && protocolId.length() > 0) {
                            protocolConfigs.add((ProtocolConfig) applicationContext.getBean(protocolId, ProtocolConfig.class));
                        }
                    }
                    serviceConfig.setProtocols(protocolConfigs);
                }
                try {
                    serviceConfig.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw (RuntimeException) e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            serviceConfig.setRef(bean);
            serviceConfigs.add(serviceConfig);
            serviceConfig.export();
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }

        Map<MethodKey, String> interfaceMap = new HashMap<MethodKey, String>();
        Map<MethodKey, List<Method>> interfaceMethodMaps = new HashMap<MethodKey, List<Method>>();
        Class<?> classType = bean.getClass();
        Class<?>[] interfaces = classType.getInterfaces();
        for (Class<?> interfaceType : interfaces) {
            Method[] methods = interfaceType.getDeclaredMethods();
            for (Method method : methods) {
                String[] parameterTypes = Mapping.getParameters(method);
                MethodKey key = new MethodKey(method);
                String interfaceName = null;
                if (interfaceMap.containsKey(key)) {
                    interfaceName = interfaceMap.get(key);
                    interfaceName += ", " + interfaceType.getName();
                } else {
                    interfaceName = interfaceType.getName();
                }
                interfaceMap.put(key, interfaceName);

                List<Method> methodList = null;
                if (interfaceMethodMaps.containsKey(key)) {
                    methodList = interfaceMethodMaps.get(key);
                } else {
                    methodList = new ArrayList<Method>();
                    interfaceMethodMaps.put(key, methodList);
                }
                methodList.add(method);

            }
        }
        Method[] methods = classType.getMethods();

        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    Reference reference = method.getAnnotation(Reference.class);
                    if (reference != null) {
                        Object value = refer(reference, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean, new Object[]{});
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Failed to init remote service reference at method " + name + " in class " + bean.getClass().getName() + ", cause: " + e.getMessage(), e);
                }
            }

            Request request = method.getAnnotation(Request.class);

            MethodKey key = new MethodKey(method);
            if (interfaceMap.containsKey(key)) {
                List<Method> methodList = interfaceMethodMaps.get(key);
                for (Method m : methodList) {
                    Mapping.push(m, method);
                }
                if (request != null) {
                    String[] args = new String[method.getTypeParameters().length];
                    String[] values = request.value();
                    Request.Method[] requestAnnotationMethods = request.method();
                    String[] requestMethods = new String[requestAnnotationMethods.length];
                    for (int i = 0; i < requestMethods.length; i++) {
                        requestMethods[i] = requestAnnotationMethods[i].name();
                    }
                    RequestMeta requestMeta = new RequestMeta();
                    requestMeta.setHeaders(request.headers());
                    requestMeta.setMethod(requestMethods);
                    for (String uri : values) {
                        if (Mapping.isMapping(uri))
                            throw new RuntimeException("Request Mapping Uri " + uri + " can't be repeated");
                        Mapping.push(method, uri, requestMeta);
                    }

                    Annotation[][] annotations = method.getParameterAnnotations();
                    if (annotations.length == 0) {
//                        Mapping.push(method, requestMeta);
                        Mapping.push(method, requestMeta,interfaceMap.get(key));
                    } else {
                        String[] parameterNames = Mapping.getParameters(method);
                        ParameterMeta[] parameterMetas = new ParameterMeta[parameterNames.length];
                        Type[] types = method.getGenericParameterTypes();
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        Map<String, ParameterMeta> parameterMetaMap = new HashMap<String, ParameterMeta>();
                        for (int i = 0; i < parameterNames.length; i++) {
                            Annotation[] parameterAnnotations = annotations[i];
                            boolean hasAnnotation = false;
                            ParameterMeta parameterMeta = new ParameterMeta();
                            String parameterName = parameterNames[i];
                            Class<?> parameterClassType = parameterTypes[i];
                            String parameterType = parameterClassType.getName();
                            for (Annotation annotation : parameterAnnotations) {
                                if (!hasAnnotation && annotation instanceof Parameter) {
                                    parameterMeta.setName(((Parameter) annotation).value());
                                    parameterMeta.setRequired(((Parameter) annotation).required());
                                    parameterMeta.setDesc(((Parameter) annotation).desc());
                                    parameterMeta.setType(((Parameter) annotation).type().name());
                                    hasAnnotation = true;
                                }
                            }
                            if (!hasAnnotation) {
                                parameterMeta.setName(parameterNames[i]);
                                parameterMeta.setRequired(true);
                                parameterMeta.setType(Mapping.getType(parameterType));
                                parameterMetas[i++] = parameterMeta;
                            }

                            parameterMeta.setRealname(parameterName);
                            parameterMeta.setParameterType(parameterType);
                            parameterMeta.setParameterTypePlus(types[i]);
                            parameterMeta.setIndex(i);
                            parameterMetas[i++] = parameterMeta;
                            parameterMetaMap.put(parameterMeta.getName(), parameterMeta);
                        }
                        requestMeta.setParameterMetas(parameterMetas);
                        Schema schema = new Schema();
                        schema.setMethodName(method.getName());
                        schema.setParameterMeta(parameterMetaMap);
                        schema.setInterfaceName(interfaceMap.get(key));
                        Mapping.push(method, schema);
                    }
                } else {
                    String[] interfaceArray = interfaceMap.get(key).split(",");
                    Mapping.push(method, interfaceArray);
                }
            }
        }

        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                Reference reference = field.getAnnotation(Reference.class);
                if (reference != null) {
                    Object value = refer(reference, field.getType());
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Throwable e) {
                logger.error("Failed to init remote service reference at filed " + field.getName() + " in class " + bean.getClass().getName() + ", cause: " + e.getMessage(), e);
            }
        }
        return bean;
    }

    private Object refer(Reference reference, Class<?> referenceClass) { //method.getParameterTypes()[0]
        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (referenceClass.isInterface()) {
            interfaceName = referenceClass.getName();
        } else {
            throw new IllegalStateException("The @Reference undefined interfaceClass or interfaceName, and the property type " + referenceClass.getName() + " is not a interface.");
        }
        String key = reference.group() + "/" + interfaceName + ":" + reference.version();
        ReferenceBean<?> referenceConfig = referenceConfigs.get(key);
        if (referenceConfig == null) {
            referenceConfig = new ReferenceBean<Object>(reference);
            if (void.class.equals(reference.interfaceClass())
                    && "".equals(reference.interfaceName())
                    && referenceClass.isInterface()) {
                referenceConfig.setInterface(referenceClass);
            }
            if (applicationContext != null) {
                referenceConfig.setApplicationContext(applicationContext);
                if (reference.registry() != null && reference.registry().length > 0) {
                    List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                    for (String registryId : reference.registry()) {
                        if (registryId != null && registryId.length() > 0) {
                            registryConfigs.add((RegistryConfig) applicationContext.getBean(registryId, RegistryConfig.class));
                        }
                    }
                    referenceConfig.setRegistries(registryConfigs);
                }
                if (reference.consumer() != null && reference.consumer().length() > 0) {
                    referenceConfig.setConsumer((ConsumerConfig) applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }
                if (reference.monitor() != null && reference.monitor().length() > 0) {
                    referenceConfig.setMonitor((MonitorConfig) applicationContext.getBean(reference.monitor(), MonitorConfig.class));
                }
                if (reference.application() != null && reference.application().length() > 0) {
                    referenceConfig.setApplication((ApplicationConfig) applicationContext.getBean(reference.application(), ApplicationConfig.class));
                }
                if (reference.module() != null && reference.module().length() > 0) {
                    referenceConfig.setModule((ModuleConfig) applicationContext.getBean(reference.module(), ModuleConfig.class));
                }
                if (reference.consumer() != null && reference.consumer().length() > 0) {
                    referenceConfig.setConsumer((ConsumerConfig) applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }
                try {
                    referenceConfig.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw (RuntimeException) e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            referenceConfigs.putIfAbsent(key, referenceConfig);
            referenceConfig = referenceConfigs.get(key);
        }
        return referenceConfig.get();
    }

    private boolean isMatchPackage(Object bean) {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        String beanClassName = bean.getClass().getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    public class MethodKey {
        private String name;
        private Class<?>[] parameterTypes;

        public MethodKey() {
        }

        public MethodKey(String name, Class<?>[] parameterTypes) {
            this.name = name;
            this.parameterTypes = parameterTypes;
        }

        public MethodKey(Method method) {
            this(method.getName(), method.getParameterTypes());
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Class<?>[] getParameterTypes() {
            return parameterTypes;
        }

        public void setParameterTypes(Class<?>[] parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof MethodKey) {
                MethodKey other = (MethodKey) obj;
                if (getName().equals(other.getName())) {
                    return equalParamTypes(parameterTypes, other.parameterTypes);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (parameterTypes.length == 0) return getName().hashCode();
            int hash = 7;
            hash = hash * 31 + getName().hashCode();
            hash = hash * 31 + parameterTypes.length;
            for (int i = 0; i < parameterTypes.length; i++) {
                hash = hash * 31 + parameterTypes[0].hashCode();
            }
            return hash;
        }

        boolean equalParamTypes(Class<?>[] params1, Class<?>[] params2) {
            if (params1.length == params2.length) {
                for (int i = 0; i < params1.length; i++) {
                    if (params1[i] != params2[i])
                        return false;
                }
                return true;
            }
            return false;
        }
    }

}
