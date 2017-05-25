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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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

    public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    protected void doOpen() throws Throwable {
        NettyHelper.setNettyLoggerFactory();
        int threads = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: " + threads);
        }
        bootstrap = new ServerBootstrap();
        if ("linux".equals(osName)) {
            boss = new EpollEventLoopGroup(threads, new NamedThreadFactory("NettyServerBoss", true));
            worker = new EpollEventLoopGroup(threads, new NamedThreadFactory("NettyServerWorker", true));
            bootstrap.group(boss, worker).channel(EpollServerSocketChannel.class);
        } else {
            boss = new NioEventLoopGroup(threads, new NamedThreadFactory("NettyServerBoss", true));
            worker = new NioEventLoopGroup(threads, new NamedThreadFactory("NettyServerWorker", true));
            bootstrap.group(boss, worker).channel(NioServerSocketChannel.class);
        }

        final NettyHandler nettyHandler = new NettyHandler(getUrl(), this);
        channels = nettyHandler.getChannels();

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("decoder", adapter.getDecoder());
                pipeline.addLast("encoder", adapter.getEncoder());
                pipeline.addLast("handler", nettyHandler);
            }
        });

//        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
//            public ChannelPipeline getPipeline() {
//                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
//                ChannelPipeline pipeline = Channels.pipeline();
//                /*int idleTimeout = getIdleTimeout();
//                if (idleTimeout > 10000) {
//                    pipeline.addLast("timer", new IdleStateHandler(timer, idleTimeout / 1000, 0, 0));
//                }*/
//                pipeline.addLast("decoder", adapter.getDecoder());
//                pipeline.addLast("encoder", adapter.getEncoder());
//                pipeline.addLast("handler", nettyHandler);
//                return pipeline;
//            }
//        });
        // bind
        channel = bootstrap.bind(getBindAddress()).channel();
//        channel.closeFuture().sync();
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