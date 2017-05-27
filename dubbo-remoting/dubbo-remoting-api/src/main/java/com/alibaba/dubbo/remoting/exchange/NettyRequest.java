package com.alibaba.dubbo.remoting.exchange;

import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;

/**
 * Created by haoning1 on 2017/5/27.
 */
public class NettyRequest {
    private DefaultHttpRequest request;
    private DefaultHttpContent content;

    public DefaultHttpRequest getRequest() {
        return request;
    }

    public void setRequest(DefaultHttpRequest request) {
        this.request = request;
    }

    public DefaultHttpContent getContent() {
        return content;
    }

    public void setContent(DefaultHttpContent content) {
        this.content = content;
    }

}
