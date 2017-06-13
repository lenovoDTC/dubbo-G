package com.alibaba.dubbo.remoting.transport.netty4;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Codec2;
import com.alibaba.dubbo.remoting.buffer.DynamicChannelBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.io.IOException;

import static com.alibaba.dubbo.remoting.transport.netty4.NettyCodecRunnable.State.decode;

/**
 * Created by haoning1 on 2017/6/6.
 */
public class NettyCodecRunnable implements  Runnable {

    private ChannelHandlerContext ctx;
    private Object msg;

    private ChannelHandler encoder;

    private ChannelHandler decoder;

    private Codec2 codec;

    private URL url;

    private int bufferSize;

    private com.alibaba.dubbo.remoting.ChannelHandler handler;

    private com.alibaba.dubbo.remoting.buffer.ChannelBuffer buffer;

    private State state;

    private ChannelOutboundHandlerAdapter outbound;

    private ChannelInboundHandlerAdapter inbound;

    private ByteBuf out;

    public NettyCodecRunnable (ChannelHandlerContext ctx, Object msg) {
        this.ctx = ctx;
        this.msg = msg;
    }

    public NettyCodecRunnable (ChannelHandlerContext ctx, Object msg, ChannelHandler encoder, com.alibaba.dubbo.remoting.ChannelHandler handler, Codec2 codec, URL url, int bufferSize, State state, ChannelOutboundHandlerAdapter outbound, ByteBuf out) {
        this(ctx, msg);
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
        this.codec = codec;
        this.url = url;
        this.bufferSize = bufferSize;
//        this.buffer = buffer;
        this.outbound = outbound;
        this.state = state;
        this.out = out;
    }

    public NettyCodecRunnable (ChannelHandlerContext ctx, Object msg, ChannelHandler decoder, com.alibaba.dubbo.remoting.ChannelHandler handler, Codec2 codec, URL url, int bufferSize, com.alibaba.dubbo.remoting.buffer.ChannelBuffer buffer, State state, ChannelInboundHandlerAdapter inbound) {
        this(ctx, msg);
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
        this.codec = codec;
        this.url = url;
        this.bufferSize = bufferSize;
        this.buffer = buffer;
        this.state = state;
        this.inbound = inbound;
    }

    @Override
    public void run() {
        try {
            if (state.equals(decode)) {
                decode(ctx, msg);
            } else {
                encode(ctx, msg, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
//            ctx.fireExceptionCaught(e.getCause());
        }

    }

    public void decode(ChannelHandlerContext ctx, Object msg) throws Exception {
        Object o = msg;
        if (!(o instanceof ByteBuf)) {
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
            ctx.pipeline().remove(inbound);
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

    public void encode (ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception{
        if (msg instanceof DefaultHttpResponse) {
            ctx.pipeline().addBefore("encoder", "httpencoder", new HttpResponseEncoder());
            ctx.pipeline().remove(outbound);
        } else {
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

    enum State {
        decode, encode
    }
}
