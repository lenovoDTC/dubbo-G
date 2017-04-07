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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import com.alibaba.fastjson.JSON;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import static org.jboss.netty.handler.codec.rtsp.RtspHeaders.Names.CONTENT_LENGTH;


/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class DecodeHandler extends AbstractChannelHandlerDelegate {

    private static final Logger log = LoggerFactory.getLogger(DecodeHandler.class);

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
        if(message instanceof DefaultHttpRequest || message instanceof DefaultHttpChunk){
            Object req = decodeRequest(channel,message);
            message = req;
        }

        handler.received(channel, message);
    }
    private Object decodeRequest(Channel channel,Object message) {
        String parem = "";
        if(message instanceof DefaultHttpRequest){
            DefaultHttpRequest httpRequst = (DefaultHttpRequest)message;
            if (httpRequst.getUri().length()>5) {
                parem = httpRequst.getUri().substring(1).replace("%7B", "{").replace("%22", "\"").replace("%7D", "}").replace("/", "\\");
            }else {
                if(httpRequst.getContent().array().length>0){
                    //                    parem = new String(httpRequst.getContent().array(),"utf-8" );
                    parem = new String(httpRequst.getContent().array());
                }else {
                    return null;
                }
            }
        }
        else if(message instanceof DefaultHttpChunk){
            DefaultHttpChunk httpChunk = (DefaultHttpChunk)message;
            try {
                parem = new String(httpChunk.getContent().array(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(parem.indexOf("{")==-1||parem.indexOf("}")==-1){
            return null;
        }
        //        localhost:20880/{"method":"sayHello","schema":"java.lang.String,int","args":"\"world\",1"}
        JSONObject jsonObject = new JSONObject(parem);
        RpcInvocation rpcInvocation = new RpcInvocation();
        Request req = new Request();
        req.setVersion("http1.0.0");
        rpcInvocation.setAttachment(Constants.DUBBO_VERSION_KEY, "2.0.0");
        rpcInvocation.setAttachment(Constants.PATH_KEY, channel.getUrl().getPath());
        rpcInvocation.setAttachment(Constants.VERSION_KEY, "0.0.0");

        rpcInvocation.setMethodName(jsonObject.getString("method"));
        try {
            Object[] args;
            List<Class<?>> ptsl = new ArrayList<Class<?>>();
            Class<?>[] pts ;
//            String[] desc = jsonObject.getString("schema").split(",");
            JSONArray jsonArray = new JSONArray(jsonObject.getString("schema"));
            JSONArray jsonArrayargs = new JSONArray(jsonObject.getString("args"));
            if (jsonArray.length() < 1) {
                pts =  null;
                args = null;
            }else if(jsonArray.length()==1&&jsonArray.getString(0).equals("")){
                pts =  null;
                args = null;
            } else {
                for(int i = 0; i<jsonArray.length();i++){
                    Class<?> pt = ReflectUtils.name2class(jsonArray.getString(i));
                    ptsl.add(pt);
                }
                pts = new Class[ptsl.size()];
                ptsl.toArray(pts);
                args = new Object[pts.length];
                for (int i = 0; i < args.length; i++) {
                   String addString =  jsonArrayargs.get(i).toString();
                    if (jsonArray.getString(i).equals("java.lang.String"))addString = "\""+addString+"\"";
                    try {
                        args[i] = JSON.parseObject(addString, pts[i]);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Decode argument failed: " + e.getMessage(), e);
                        }
                    }
                }
            }
            rpcInvocation.setParameterTypes(pts);
            Map<String, String> attachment = rpcInvocation.getAttachments();
            if (attachment == null) {
                attachment = new HashMap<String, String>();
            }
            rpcInvocation.setAttachments(attachment);
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
