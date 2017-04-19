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
import com.alibaba.dubbo.remoting.http.Mapping;
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
        String param = "";
        Map<String,Object> parameter = new HashMap<String, Object>();
        if(message instanceof DefaultHttpRequest){
            DefaultHttpRequest httpRequst = (DefaultHttpRequest)message;
            if (httpRequst.getMethod().getName().equals("GET")){
                if(Mapping.isMapping(httpRequst.getUri().substring(0,httpRequst.getUri().indexOf("?")))){
                    String[] parameters = httpRequst.getUri().substring(httpRequst.getUri().indexOf("?")+1).split("&");
                    for(String p : parameters){
                        String[] ps = p.split("=");
                        if(parameter.containsKey(ps[0])){
                            parameter.put(ps[0],parameter.get(ps[0]).toString()+"&"+ps[1]);
                        }else parameter.put(ps[0],ps[1]);
                    }
                    for(String p : parameters){
                        String[] ps = p.split("=");
                        if (parameter.get(ps[0]).toString().contains("&"))parameter.put(ps[0],parameter.get(ps[0]).toString().split("&"));
                    }
                try {
                    param = Mapping.decode(httpRequst.getUri().substring(0,httpRequst.getUri().indexOf("?")),parameter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                }else
                param = httpRequst.getUri().substring(1).replace("%7B", "{").replace("%22", "\"").replace("%7D", "}").replace("/", "\\");
            }else if(httpRequst.getMethod().getName().equals("POST")){
//                                        parem = new String(httpRequst.getContent().array(),"utf-8" );
                if(Mapping.isMapping(httpRequst.getUri().substring(0,httpRequst.getUri().indexOf("?")))){
                    String uri = httpRequst.getUri().substring(httpRequst.getUri().indexOf("?")+1) + "&" +new String(httpRequst.getContent().array());
                    String[] parameters = uri.split("&");
                    for(String p : parameters){
                        String[] ps = p.split("=");
                        if(parameter.containsKey(ps[0])){
                            parameter.put(ps[0],parameter.get(ps[0]).toString()+"&"+ps[1]);
                        }else parameter.put(ps[0],ps[1]);
                    }
                    for(String p : parameters){
                        String[] ps = p.split("=");
                        if (parameter.get(ps[0]).toString().contains("&"))parameter.put(ps[0],parameter.get(ps[0]).toString().split("&"));
                    }
                    try {
                        param = Mapping.decode(httpRequst.getUri().substring(0,httpRequst.getUri().indexOf("?")),parameter);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }else param = new String(httpRequst.getContent().array());
            }
        }
        else if(message instanceof DefaultHttpChunk){
            DefaultHttpChunk httpChunk = (DefaultHttpChunk)message;
            try {
                param = new String(httpChunk.getContent().array(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if(param.indexOf("{")==-1||param.indexOf("}")==-1){
            return null;
        }
        System.out.println(param);
        //        localhost:20880/{"interface":"com.alibaba.dubbo.demo.DemoService","method":"sayHello","schema":"[java.lang.String,int]","args":"[\"world\",1]"}
        JSONObject jsonObject = new JSONObject(param);
        RpcInvocation rpcInvocation = new RpcInvocation();
        Request req = new Request();
        req.setVersion("http1.0.0");
        rpcInvocation.setAttachment(Constants.DUBBO_VERSION_KEY, "2.0.0");
//        rpcInvocation.setAttachment(Constants.PATH_KEY, channel.getUrl().getPath());
        rpcInvocation.setAttachment(Constants.PATH_KEY, jsonObject.getString("interface"));
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
