package com.alibaba.dubbo.remoting.exchange;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by haoning1 on 2017/6/15.
 */
public class NettyResponse {

    private int status;
    private String content;
    private String version;
    private String connection;

    private Map<String, String> headers;
    private Map<String, Cookie> cookies;

    public NettyResponse () {
        this.headers = new LinkedHashMap<String, String>();
        this.cookies = new LinkedHashMap<String, Cookie>();
    }

    public NettyResponse (int status, String version) {
        this();
        this.status = status;
        this.version = version;
    }

    public NettyResponse (int status, String version, String content) {
        this(status, version);
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }

    public void addHeader (String name, String value) {
        headers.put(name, value);
    }

    public void addCookie (String name, String value) {
        Cookie cookie = new NettyCookie(name, value);
        addCookie(name, cookie);
    }

    public void addCookie (String name, Cookie cookie) {
        cookies.put(name, cookie);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }
}
