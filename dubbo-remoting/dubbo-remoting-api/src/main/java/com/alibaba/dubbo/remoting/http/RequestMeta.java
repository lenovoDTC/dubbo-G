package com.alibaba.dubbo.remoting.http;

/**
 * Created by haoning1 on 2017/4/6.
 */
public class RequestMeta {
    private String[] uri;
    private String[] method;
    private String[] headers;

    private ParameterMeta[] parameterMetas;

    public String[] getUri() {
        return uri;
    }

    public void setUri(String[] uri) {
        this.uri = uri;
    }

    public String[] getMethod() {
        return method;
    }

    public void setMethod(String[] method) {
        this.method = method;
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    public ParameterMeta[] getParameterMetas() {
        return parameterMetas;
    }

    public void setParameterMetas(ParameterMeta[] parameterMetas) {
        this.parameterMetas = parameterMetas;
    }
}
