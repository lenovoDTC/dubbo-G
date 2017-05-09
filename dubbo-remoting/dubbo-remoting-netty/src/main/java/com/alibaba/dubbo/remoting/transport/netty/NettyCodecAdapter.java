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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.DynamicChannelBuffer;

/**
 * NettyCodecAdapter.
 *
 * @author qian.lei
 */
final class NettyCodecAdapter {

    private final ChannelHandler encoder = new InternalEncoder();

    private final ChannelHandler decoder = new InternalDecoder();

    private final Codec2 codec;

    private final URL url;

    private final int bufferSize;

    private final com.alibaba.dubbo.remoting.ChannelHandler handler;

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

    @Sharable
    private class InternalEncoder extends OneToOneEncoder {

        @Override
        protected Object encode(ChannelHandlerContext ctx, Channel ch, Object msg) throws Exception {
            if (msg instanceof DefaultHttpResponse) {
                ctx.getPipeline().addBefore("encoder", "httpencoder", new HttpResponseEncoder());
                ctx.getPipeline().remove(this);
                return msg;
            } else {
                com.alibaba.dubbo.remoting.buffer.ChannelBuffer buffer =
                        com.alibaba.dubbo.remoting.buffer.ChannelBuffers.dynamicBuffer(1024);
                NettyChannel channel = NettyChannel.getOrAddChannel(ch, url, handler);
                try {
                    codec.encode(channel, buffer, msg);
                } finally {
                    NettyChannel.removeChannelIfDisconnected(ch);
                }
                return ChannelBuffers.wrappedBuffer(buffer.toByteBuffer());
            }
        }
    }

    private class InternalDecoder extends SimpleChannelUpstreamHandler {

        private com.alibaba.dubbo.remoting.buffer.ChannelBuffer buffer =
                com.alibaba.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
            Object o = event.getMessage();
            ChannelBuffer input = (ChannelBuffer) o;
            if (!(o instanceof ChannelBuffer)) {
                ctx.sendUpstream(event);
                return;
            }
//            if((input.getByte(0)==71&&input.getByte(1)==69&&input.getByte(2)==84)||(input.getByte(0)==80&&input.getByte(1)==79&&input.getByte(2)==83&&input.getByte(3)==84)) {
            if (new String(input.array(), "UTF-8").indexOf("HTTP/1.1\r\n") != -1) {
                ctx.getPipeline().addAfter("decoder", "httpdecoder", new HttpRequestDecoder());
                ctx.getPipeline().remove(this);
                ctx.sendUpstream(event);
                return;
            } else {
                int readable = input.readableBytes();
                if (readable <= 0) {
                    return;
                }
                com.alibaba.dubbo.remoting.buffer.ChannelBuffer message;
                if (buffer.readable()) {
                    if (buffer instanceof DynamicChannelBuffer) {
                        buffer.writeBytes(input.toByteBuffer());
                        message = buffer;
                    } else {
                        int size = buffer.readableBytes() + input.readableBytes();
                        message = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.dynamicBuffer(
                                size > bufferSize ? size : bufferSize);
                        message.writeBytes(buffer, buffer.readableBytes());
                        message.writeBytes(input.toByteBuffer());
                    }
                } else {
                    message = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.wrappedBuffer(
                            input.toByteBuffer());
                }
                NettyChannel channel = NettyChannel.getOrAddChannel(ctx.getChannel(), url, handler);
                Object msg;

                int saveReaderIndex;

                try {

                    // decode object.
                    do {
                        saveReaderIndex = message.readerIndex();
                        try {
                            msg = codec.decode(channel, message);
                        } catch (IOException e) {
                            buffer = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;
                            throw e;
                        }
                        if (msg == Codec2.DecodeResult.NEED_MORE_INPUT) {
                            message.readerIndex(saveReaderIndex);
                            break;
                        } else {
                            if (saveReaderIndex == message.readerIndex()) {
                                buffer = com.alibaba.dubbo.remoting.buffer.ChannelBuffers.EMPTY_BUFFER;
                                throw new IOException("Decode without read data.");
                            }
                            if (msg != null) {
                                Channels.fireMessageReceived(ctx, msg, event.getRemoteAddress());
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
                    NettyChannel.removeChannelIfDisconnected(ctx.getChannel());
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            ctx.sendUpstream(e);
        }
    }
}