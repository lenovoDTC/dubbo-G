/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.*;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.rpc.RpcInvocation;
import org.jboss.netty.handler.codec.http.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.alibaba.fastjson.JSON;
import org.json.JSONObject;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class DecodeHandler extends AbstractChannelHandlerDelegate {

    private static final Logger log = LoggerFactory.getLogger(DecodeHandler.class);

    private static final Map<String,String[]> methodPtypeMap = new HashMap<String, String[]>();


    public DecodeHandler(ChannelHandler handler) {
        super(handler);
    }

    public void received(Channel channel, Object message) throws RemotingException {
        if (message instanceof Decodeable) {
            decode(message);
        }

        if (message instanceof Request) {
            decode(((Request) message).getData());
        }

        if (message instanceof Response) {
            decode(((Response) message).getResult());
        }
        if(message instanceof DefaultHttpRequest){
            Object req = decodeRequest(channel,message);
            message = req;
        }
        handler.received(channel, message);
    }
    private Object decodeRequest(Channel channel,Object message) {
        DefaultHttpRequest httpRequst = (DefaultHttpRequest)message;
        String uri = httpRequst.getUri();
        JSONObject jsonObject = new JSONObject(uri);
//        Method[] a = SpringContainer.getContext().getBean(channel.getUrl().getPath()).getClass();
        RpcInvocation rpcInvocation = new RpcInvocation();
        Request req = new Request();
        req.setVersion("http1.0.0");
        rpcInvocation.setAttachment(Constants.DUBBO_VERSION_KEY, "2.0.0");
        rpcInvocation.setAttachment(Constants.PATH_KEY, channel.getUrl().getPath());
        rpcInvocation.setAttachment(Constants.VERSION_KEY, "0.0.0");

        rpcInvocation.setMethodName(jsonObject.getString("method"));
            try {
                Object[] args;
                Class<?>[] pts = new Class<?>[0];
                String[] desc = (String[]) jsonObject.get("schema");
                if (desc.length == 0) {
                    pts =  null;
                    args = null;
                } else {
                    for(String des : desc){
                    Class<?> pt = ReflectUtils.name2class(des);
                        Arrays.fill(pts,pt);
                    }
                    args = new Object[pts.length];
                    for (int i = 0; i < args.length; i++) {
                        try {
                            args[i] = JSON.parseObject(((String[])jsonObject.get("args"))[i], pts[i]);
                        } catch (Exception e) {
                            if (log.isWarnEnabled()) {
                                log.warn("Decode argument failed: " + e.getMessage(), e);
                            }
                        }
                    }
                }
                rpcInvocation.setParameterTypes(pts);

//            Map<String, String> map = (Map<String, String>) in.readObject(Map.class);
                Map<String,String> map = new HashMap<String, String>();
                if (map != null && map.size() > 0) {
                    Map<String, String> attachment = rpcInvocation.getAttachments();
                    if (attachment == null) {
                        attachment = new HashMap<String, String>();
                    }
                    attachment.putAll(map);
                    rpcInvocation.setAttachments(attachment);
                }
                rpcInvocation.setArguments(args);
                req.setData(rpcInvocation);
            } catch (ClassNotFoundException e) {
                try {
                    throw new IOException(StringUtils.toString("Read invocation data failed.", e));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return  req;
    }
    private void decode(Object message)  {
        if (message != null && message instanceof Decodeable) {
            try {
                ((Decodeable) message).decode();
                if (log.isDebugEnabled()) {
                    log.debug(new StringBuilder(32).append("Decode decodeable message ")
                            .append(message.getClass().getName()).toString());
                }
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn(
                            new StringBuilder(32)
                                    .append("Call Decodeable.decode failed: ")
                                    .append(e.getMessage()).toString(),
                            e);
                }
            } // ~ end of catch
        } // ~ end of if
    } // ~ end of method decode
}
