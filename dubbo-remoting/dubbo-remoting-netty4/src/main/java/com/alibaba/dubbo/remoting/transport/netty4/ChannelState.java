package com.alibaba.dubbo.remoting.transport.netty4;

import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

/**
 * Created by haoning1 on 2017/6/22.
 */
public class ChannelState {
    private Object message;
    private boolean isFinished = true;
    public Object getMessage() {
        return message;
    }
    private HttpPostRequestDecoder decoder;

    public void setMessage(Object message) {
        this.message = message;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public HttpPostRequestDecoder getDecoder() {
        return decoder;
    }

    public void setDecoder(HttpPostRequestDecoder decoder) {
        this.decoder = decoder;
    }

    public void clear () {
        if (decoder != null) {
            decoder.cleanFiles();
            decoder.destroy();
        }
        isFinished = true;
    }
}
