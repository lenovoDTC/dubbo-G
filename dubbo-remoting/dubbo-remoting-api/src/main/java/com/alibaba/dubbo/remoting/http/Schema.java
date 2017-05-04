package com.alibaba.dubbo.remoting.http;

import java.util.Map;

/**
 * Created by haoning1 on 2017/4/6.
 */
public class Schema {
    private String methodName;
    private String interfaceName;

    private RequestMeta requestMeta;

    private Map<String, ParameterMeta> parameterMeta;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Map<String, ParameterMeta> getParameterMeta() {
        return parameterMeta;
    }

    public void setParameterMeta(Map<String, ParameterMeta> parameterMeta) {
        this.parameterMeta = parameterMeta;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public RequestMeta getRequestMeta() {
        return requestMeta;
    }

    public void setRequestMeta(RequestMeta requestMeta) {
        this.requestMeta = requestMeta;
    }
}
