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
package com.alibaba.dubbo.remoting.transport.netty4;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.exchange.NettyCookie;
import com.alibaba.dubbo.remoting.exchange.NettyRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NettyHandler
 *
 * @author william.liangf
 */
@io.netty.channel.ChannelHandler.Sharable
public class NettyHandler extends ChannelInboundHandlerAdapter {

    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>(); // <ip:port, channel>

    private final URL url;

    private final ChannelHandler handler;

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

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
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (decoder != null) {
            decoder.cleanFiles();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            if (channel != null) {
                channels.put(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()), channel);
            }
            handler.connected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            channels.remove(NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress()));
            handler.disconnected(channel);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            isFinished = false;
            HttpRequest request = (HttpRequest) msg;
//            String uri = request.getUri();
            URI uri = new URI(request.getUri());
            String reqeustUri = uri.getPath();
            message = nRequest = new NettyRequest(reqeustUri, request.getMethod().name());
            HttpHeaders headers = request.headers();
            Iterator<Map.Entry<String, String>> it = headers.iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                nRequest.addHeader(entry.getKey(), entry.getValue());
            }

            Set<Cookie> cookies = null;
            String cookie = headers.get("COOKIE");
            if (cookie != null) {
                cookies = CookieDecoder.decode(cookie);
                for (Cookie c : cookies) {
                    com.alibaba.dubbo.remoting.exchange.Cookie nCookie = new NettyCookie(c.getName(), c.getValue());
                    nCookie.setComment(c.getComment());
                    nCookie.setCommentUrl(c.getCommentUrl());
                    nCookie.setDomain(c.getDomain());
                    nCookie.setHttpOnly(c.isHttpOnly());
                    nCookie.setDiscard(c.isDiscard());
                    nCookie.setMaxAge(c.getMaxAge());
                    nCookie.setVersion(c.getVersion());
                    nCookie.setPath(c.getPath());
                    nCookie.setPorts(c.getPorts());
                    nRequest.addCookie(nCookie);
                }
            }

            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
            Map<String, List<String>> parameters = queryStringDecoder.parameters();
            nRequest.addAllParameters(parameters);

            if (nRequest.getMethod().equals("GET")) {
                isFinished = true;
            }

            if (nRequest.getMethod().equals("POST")) {
                try {

                    decoder = new HttpPostRequestDecoder(factory, request);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e1) {
                    e1.printStackTrace();
                    ctx.channel().close();
                    return;
                }

            }
        } else if (msg instanceof HttpContent) {
            if (decoder != null) {
                HttpContent chunk = (HttpContent) msg;
                try {
                    decoder.offer(chunk);
                } catch (HttpPostRequestDecoder.ErrorDataDecoderException e) {
                    ctx.channel().close();
                    return;
                }

                readHttpDataChunkByChunk();

                if (chunk instanceof LastHttpContent) {
                    reset();
                }
            }
        } else message = msg;

        if (isFinished) {
            NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
            try {
                handler.received(channel, message);
            } finally {
                NettyChannel.removeChannelIfDisconnected(ctx.channel());
            }
        }

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
                        data.release();
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
            // end
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
        try {
            handler.caught(channel, cause);
        } finally {
            NettyChannel.removeChannelIfDisconnected(ctx.channel());
        }
    }

    private void reset() {
        nRequest = null;
        isFinished = true;
        // destroy the decoder to release all resources
        decoder.destroy();
        decoder = null;
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
}