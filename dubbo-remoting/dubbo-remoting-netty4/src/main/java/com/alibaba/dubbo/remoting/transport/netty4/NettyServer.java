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
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ExecutorUtil;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.transport.AbstractServer;
import com.alibaba.dubbo.remoting.transport.dispatcher.ChannelHandlers;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

/**
 * NettyServer
 *
 * @author qian.lei
 * @author chao.liuc
 */
public class NettyServer extends AbstractServer implements Server {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private static final String osName = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();

    private Map<String, Channel> channels; // <ip:port, channel>

    private ServerBootstrap bootstrap;

    EventLoopGroup boss = null;
    EventLoopGroup worker = null;

    private io.netty.channel.Channel channel;

    private final int WORKER_THREADS = SystemPropertyUtil.getInt("io.netty.worker", 8);

    private final EventExecutorGroup executor = new DefaultEventExecutorGroup(WORKER_THREADS, new NamedThreadFactory("NettyExecutorGroup", true));


    public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    protected void doOpen() throws Throwable {
        NettyHelper.setNettyLoggerFactory();
        int threads = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() + 1));
        boolean epoll = SystemPropertyUtil.getBoolean("io.netty.epoll", true);
        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: " + threads);
            logger.debug("-Dio.netty.epoll: " + epoll);
        }
        bootstrap = new ServerBootstrap();
        if ("linux".equals(osName) && epoll) {
            boss = new EpollEventLoopGroup(threads, new NamedThreadFactory("NettyServerBoss-Linux", true));
            worker = new EpollEventLoopGroup(WORKER_THREADS, new NamedThreadFactory("NettyServerWorker-Linux", true));
            bootstrap
                    .group(boss, worker)
                    .channel(EpollServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(EpollChannelOption.SO_REUSEPORT, true)
                    .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 32 * 1024)
                    .childOption(ChannelOption.SO_RCVBUF, 128 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 128 * 1024);
        } else {
            boss = new NioEventLoopGroup(threads, new NamedThreadFactory("NettyServerBoss-Windows", true));
            worker = new NioEventLoopGroup(WORKER_THREADS, new NamedThreadFactory("NettyServerWorker-Windows", true));
            bootstrap
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 32 * 1024)
                    .childOption(ChannelOption.SO_RCVBUF, 1024 * 128)
                    .childOption(ChannelOption.SO_SNDBUF, 1024 * 128);

        }

        final NettyHandler nettyHandler = new NettyHandler(getUrl(), this);
        channels = nettyHandler.getChannels();

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(executor, "decoder", adapter.getDecoder());
                pipeline.addLast(executor, "encoder", adapter.getEncoder());
                pipeline.addLast(executor, "handler", new NettyHandler(getUrl(), NettyServer.this));

            }
        });

        channel = bootstrap.bind(getBindAddress()).channel();
    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            Collection<Channel> channels = getChannels();
            if (channels != null && channels.size() > 0) {
                for (Channel channel : channels) {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public Collection<Channel> getChannels() {
        Collection<Channel> chs = new HashSet<Channel>();
        for (Channel channel : this.channels.values()) {
            if (channel.isConnected()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtils.toAddressString(channel.getRemoteAddress()));
            }
        }
        return chs;
    }

    public Channel getChannel(InetSocketAddress remoteAddress) {
        return channels.get(NetUtils.toAddressString(remoteAddress));
    }

    public boolean isBound() {
        return channel.isRegistered();
    }

}