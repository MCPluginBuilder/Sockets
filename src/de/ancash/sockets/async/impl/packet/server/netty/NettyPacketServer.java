package de.ancash.sockets.async.impl.packet.server.netty;

import java.util.HashSet;
import java.util.Set;

import de.ancash.libs.org.bukkit.event.EventManager;
import de.ancash.sockets.async.impl.packet.client.netty.NettyPacketClient;
import de.ancash.sockets.async.impl.packet.netty.NettyPacketReadHandler;
import de.ancash.sockets.async.impl.packet.server.AsyncPacketServerClient;
import de.ancash.sockets.events.ClientDisconnectEvent;
import de.ancash.sockets.events.ServerPacketReceiveEvent;
import de.ancash.sockets.io.ITCPClient;
import de.ancash.sockets.netty.NettyClient;
import de.ancash.sockets.packet.Packet;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyPacketServer {

	private final Set<ITCPClient> clients = new HashSet<>();

	public NettyPacketServer(String address, int port) throws InterruptedException {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
              .channel(NioServerSocketChannel.class)
              .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) 
                  throws Exception {
        			NettyClient cl = new NettyClient(ch);
        			NettyPacketClient npc = new NettyPacketClient(cl, null ,() -> onDisconnect(cl, null));
        			npc.setOnPacket(p -> onPacket(p, npc));
        			synchronized (clients) {
        				clients.add(npc);
					}
        			System.out.println(ch.remoteAddress() + " connected! (" + clients.size() + ")");
                    ch.pipeline().addLast(new NettyPacketReadHandler(p -> npc.onPacket(p), () -> onDisconnect(npc, null)));
                }
            }).childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
	}

	protected final void onPacket(Packet packet, ITCPClient client) {
		EventManager.callEvent(new ServerPacketReceiveEvent(packet, client));
	}

	void onDisconnect(ITCPClient cl, Throwable th) {

		synchronized (clients) {
			clients.remove(cl);
		}
		System.out.println(cl.getRemoteAddress() + " disconnected! (" + clients.size() + ")");
		EventManager.callEvent(new ClientDisconnectEvent(cl, th));
	}

	public void writeAllExcept(Packet reconstructed, AsyncPacketServerClient sender) throws InterruptedException {
		synchronized (clients) {
			for (ITCPClient cl : clients)
				if (!cl.equals(sender))
					cl.putWrite(reconstructed.toBytes());
		}
	}
}
