package com.alibaba.dubbo.remoting.exchange;

import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;

import java.util.*;

/**
 * Created by haoning1 on 2017/5/27.
 */
public class NettyRequest {

    private String uri;
    private String method;

    private Map<String, List<String>> parameters;
    private Map<String, String> headers;
    private Map<String, Cookie> cookies;

    public NettyRequest () {
        parameters = new LinkedHashMap<String, List<String>>();
        headers = new LinkedHashMap<String, String>();
        cookies = new LinkedHashMap<String, Cookie>();
    }

    public NettyRequest (String uri, String method) {
        this();
        this.uri = uri;
        this.method = method;
    }

    public void addHeader (String name, String value) {
        headers.put(name, value);
    }

    public void addCookie (String name, String value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is empty");
        Cookie cookie = new NettyCookie(name, value);
        cookies.put(name, cookie);
    }

    public void addCookie (Cookie cookie) {
        if (cookie == null || cookie.getName() == null || cookie.getName().isEmpty())
            throw new IllegalArgumentException("cookie is empty");
        cookies.put(cookie.getName(), cookie);
    }

    public void addParameter(String name, String value) {
        List<String> list = null;
        if (parameters.containsKey(name)) {
            list = parameters.get(name);
        } else {
            list = new ArrayList<String>();
            addParameter(name, list);
        }
        list.add(value);
    }

    public void addParameter (String name, List<String> value) {
        parameters.put(name, value);
    }

    public void addAllParameters (Map<String, List<String>> parameters) {
        this.parameters.putAll(parameters);
    }

    public String getMethod () {
        return this.method;
    }

    public String getUri () {
        return this.uri;
    }

    public Map<String, List<String>> getParameters() {
        return this.parameters;
    }
}
