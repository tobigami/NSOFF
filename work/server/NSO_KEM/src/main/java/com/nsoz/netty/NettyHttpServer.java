package com.nsoz.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

public class NettyHttpServer {
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    public NettyHttpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
         bossGroup = new NioEventLoopGroup(1);
         workerGroup = new NioEventLoopGroup(1);
        try {
            ManagementService managementService = new ManagementService();

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new ChunkedWriteHandler());
                            p.addLast(new HttpObjectAggregator(1048576)); // 1 MB là kích thước tối đa của HTTP message
                            p.addLast(new RequestHandler(managementService));
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
//            System.out.println("Server started, listening on " + port);
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    public void shutdownServer() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        System.out.println("Server has been stopped.");
    }
}
