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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.DynamicChannelBuffer;
import com.alibaba.dubbo.remoting.exchange.NettyResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;

/**
 * NettyCodecAdapter.
 *
 * @author qian.lei
 */
final class NettyCodecAdapter {

    private final ChannelHandler encoder = new InternalEncoder();

    private final ChannelHandler decoder = new InternalDecoder();

    private final ChannelHandler httpEncoder = new HttpResponseEncoder();

    private final ChannelHandler dubboEncoder = new InternalDubboEncoder();

    private final Codec2 codec;

    private final URL url;

    private final int bufferSize;

    private final com.alibaba.dubbo.remoting.ChannelHandler handler;

    private ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("DubboCodecSharedHandler", true));

    public NettyCodecAdapter(Codec2 codec, URL url, com.alibaba.dubbo.remoting.ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
        int b = url.getPositiveParameter(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= Constants.MIN_BUFFER_SIZE && b <= Constants.MAX_BUFFER_SIZE ? b : Constants.DEFAULT_BUFFER_SIZE;
    }

    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    private class InternalEncoder extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            Object message = msg;
            if (msg instanceof  NettyResponse) {
                NettyResponse nResponse = (NettyResponse) msg;
                ByteBuf buf = copiedBuffer(nResponse.getContent().toString(), CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
                response.headers().set(CONTENT_TYPE, nResponse.getHeaders().get(CONTENT_TYPE));

                boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(nResponse.getConnection())
                        || nResponse.getVersion().equals(HttpVersion.HTTP_1_0)
                        && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(nResponse.getConnection());
                if (!close) {
                    response.headers().set(CONTENT_LENGTH, buf.readableBytes());
                }

                Set<Cookie> cookies;
                String value = nResponse.getHeaders().get(COOKIE);
                if (value == null) {
                    cookies = Collections.emptySet();
                } else {
                    cookies = CookieDecoder.decode(value);
                }
                if (!cookies.isEmpty()) {
                    for (Cookie cookie : cookies) {
                        response.headers().add(SET_COOKIE, ServerCookieEncoder.encode(cookie));
                    }
                }

                Map<String, String> headers = nResponse.getHeaders();
                for (String name : headers.keySet()) {
                    response.headers().add(name, headers.get(name));
                }
                message = response;
                ctx.pipeline().addBefore("encoder", "httpencoder", httpEncoder);
            } else {
                ctx.pipeline().addBefore("encoder", "dubboEncoder", dubboEncoder);
            }

            super.write(ctx, message, promise);
        }
    }

    @ChannelHandler.Sharable
    private class InternalDubboEncoder extends MessageToByteEncoder<Object> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            com.alibaba.dubbo.remoting.buffer.ChannelBuffer buffer =
                    com.alibaba.dubbo.remoting.buffer.ChannelBuffers.dynamicBuffer(1024);
            NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
            try {
                codec.encode(channel, buffer, msg);
            } finally {
                NettyChannel.removeChannelIfDisconnected(ctx.channel());
            }
            out.writeBytes(buffer.toByteBuffer());
        }
    }

    private class InternalDecoder extends ChannelInboundHandlerAdapter {

        private com.alibaba.dubbo.remoting.buffer.ChannelBuffer buffer =
                com.alibaba.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//            System.out.println("get request : " + System.currentTimeMillis());
//            executor.execute(new NettyCodecRunnable(ctx, msg, decoder, handler, codec, url, bufferSize, buffer, NettyCodecRunnable.State.decode, this));
            Object o = msg;
            if (!(o instanceof  ByteBuf)) {
                ctx.fireChannelRead(msg);
                return;
            }

            ByteBuf input = (ByteBuf) o;
            int readable = input.readableBytes();
            if (readable <= 0)
                return;
            byte[] array = new byte[readable];
            input.getBytes(input.readerIndex(), array);
            if (new String(array, "UTF-8").indexOf("HTTP/1.1\r\n") != -1) {
                ctx.pipeline().addAfter("decoder", "httpdecoder", new HttpRequestDecoder());
                ctx.pipeline().remove(this);
                ctx.fireChannelRead(msg);
            } else {
                com.alibaba.dubbo.remoting.buffer.ChannelBuffer message;
                if (buffer.readable()) {
                    if (buffer instanceof DynamicChannelBuffer) {
                        buffer.writeBytes(input.nioBuffer());
                        message = buffer;
                    } else {
                        int size = buffer.readableBytes() + input.readableBytes();
                        message = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.dynamicBuffer(
                                size > bufferSize ? size : bufferSize);
                        message.writeBytes(buffer, buffer.readableBytes());
                        message.writeBytes(input.nioBuffer());
                    }
                } else {
                    message = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.wrappedBuffer(
                            input.nioBuffer());
                }

                NettyChannel channel = NettyChannel.getOrAddChannel(ctx.channel(), url, handler);
                Object _msg;

                int saveReaderIndex;

                try {

                    // decode object.
                    do {
                        saveReaderIndex = message.readerIndex();
                        try {
                            _msg = codec.decode(channel, message);
                        } catch (IOException e) {
                            buffer = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;
                            throw e;
                        }
                        if (_msg == Codec2.DecodeResult.NEED_MORE_INPUT) {
                            message.readerIndex(saveReaderIndex);
                            break;
                        } else {
                            if (saveReaderIndex == message.readerIndex()) {
                                buffer = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;
                                throw new IOException("Decode without read data.");
                            }
                            if (msg != null) {
                                ctx.fireChannelRead(_msg);
                            }
                        }
                    } while (message.readable());

                } finally {
                    if (message.readable()) {
                        message.discardReadBytes();
                        buffer = message;
                    } else {
                        buffer = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;
                    }
                    NettyChannel.removeChannelIfDisconnected(ctx.channel());
                }
            }
        }
    }
}