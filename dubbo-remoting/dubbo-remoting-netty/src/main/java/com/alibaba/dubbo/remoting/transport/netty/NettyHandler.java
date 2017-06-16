/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.remoting.transport.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.exchange.NettyCookie;
import com.alibaba.dubbo.remoting.exchange.NettyRequest;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.cookie.*;
import org.jboss.netty.handler.codec.http.cookie.Cookie;
import org.jboss.netty.handler.codec.http.multipart.*;

import static sun.plugin.ClassLoaderInfo.reset;

/**
 * NettyHandler
 *
 * @author william.liangf
 */
@org.jboss.netty.channel.ChannelHandler.Sharable
public class NettyHandler extends SimpleChannelHandler {

    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>(); // <ip:port, channel>

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

    private final URL url;

    private final ChannelHandler handler;

    private HttpPostRequestDecoder decoder;

    private NettyRequest nRequest;

    private Object message;

    private boolean isFinished = true;

    public NettyHandler(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
        try {
            if (channel != null) {
                channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.getChannel().getRemoteAddress()), channel);
            }
            handler.connected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
        }
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
        try {
            channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.getChannel().getRemoteAddress()));
            handler.disconnected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
//        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
        if (e.getMessage() instanceof HttpRequest) {
            isFinished = false;
            HttpRequest request = (HttpRequest) e.getMessage();
//            String uri = request.getUri();
            URI uri = new URI(request.getUri());
            String reqeustUri = uri.getPath();
            message = nRequest = new NettyRequest(reqeustUri, request.getMethod().getName(),request.getProtocolVersion().getText());
            HttpHeaders headers = request.headers();
            Iterator<Map.Entry<String, String>> it = headers.iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                nRequest.addHeader(entry.getKey(), entry.getValue());
            }

            Set<Cookie> cookies = null;
            String cookie = headers.get("COOKIE");
            if (cookie != null) {
                cookies = ServerCookieDecoder.STRICT.decode(cookie);
                for (Cookie c : cookies) {
                    com.alibaba.dubbo.remoting.exchange.Cookie nCookie = new NettyCookie(c.name(), c.value());
//                    nCookie.setComment(c.getComment());
//                    nCookie.setCommentUrl(c.getCommentUrl());
                    nCookie.setDomain(c.domain());
                    nCookie.setHttpOnly(c.isHttpOnly());
//                    nCookie.setDiscard(c.isDiscard());
                    nCookie.setMaxAge(c.maxAge());
//                    nCookie.setVersion(c.getVersion);
                    nCookie.setPath(c.path());
//                    nCookie.setPorts(c.getPorts());
                    nRequest.addCookie(nCookie);
                }
            }

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            Map<String, List<String>> parameters = queryStringDecoder.getParameters();
            nRequest.addAllParameters(parameters);

            if (nRequest.getMethod().equals("GET")) {
                isFinished = true;
            }

            if (nRequest.getMethod().equals("POST")) {
                try {

                    decoder = new HttpPostRequestDecoder(factory, request);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                    e1.printStackTrace();
                    ctx.getChannel().close();
                    return;
                }

            }
        }
         else if (e.getMessage() instanceof HttpChunk) {
            if (decoder != null) {
                HttpChunk chunk = (HttpChunk) e.getMessage();
                try {
                    decoder.offer(chunk);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                    ctx.getChannel().close();
                    return;
                }

                readHttpDataChunkByChunk();

                if (chunk.isLast())  {
                    reset();
                }
            }
        }
        else message = e.getMessage();

        if (isFinished) {
            NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
            try {
                handler.received(channel, message);
            } finally {
                NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
            }
        }
    }
    private void reset() {
        nRequest = null;
        isFinished = true;
        // destroy the decoder to release all resources
//        decoder.;
        decoder = null;
    }
    /**
     * Example of reading request by chunk and getting values from chunk to chunk
     */
    private void readHttpDataChunkByChunk() {
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    try {
                        readData(data);
                    } finally {
//                        data.release();
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
            // end
        }
    }
    private void readData (InterfaceHttpData data) {
        if (!data.getHttpDataType().equals(InterfaceHttpData.HttpDataType.Attribute)) {
            return;
        }

        Attribute attribute = (Attribute) data;
        try {
            nRequest.addParameter(attribute.getName(), attribute.getValue());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //if (data.getHttpDataType().equals(InterfaceHttpData.HttpDataType.))
    }



    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        super.writeRequested(ctx, e);
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
        try {
            handler.sent(channel, e.getMessage());
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
        try {
            handler.caught(channel, e.getCause());
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
        }
    }

}