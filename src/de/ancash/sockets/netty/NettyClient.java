package de.ancash.sockets.netty;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import de.ancash.sockets.async.impl.packet.netty.NettyPacketReadHandler;
import de.ancash.sockets.io.ITCPClient;
import de.ancash.sockets.packet.Packet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient implements ITCPClient{

	public static void main(String[] args) throws InterruptedException {
		NettyClient cl = new NettyClient("localhost", 25000);
		cl.setOnConnect(() -> System.out.println("cn"));
		cl.setOnDisconnect(() -> System.out.println("dc"));
		cl.connect(new NettyPacketReadHandler(null, cl.onDisconnect));
	}
	
	private final String host;
	private final int port;
	private SocketChannel channel;
	private Runnable onConnect;
	private Runnable onDisconnect;
	
	public NettyClient(String host, int port) {
		this.port = port;
		this.host = host;
	}
	
	public NettyClient(SocketChannel channel) {
		this.host = null;
		this.port = -1;
		this.channel = channel;
	}
	
	public void setOnDisconnect(Runnable onDisconnect) {
		this.onDisconnect = onDisconnect;
	}
	
	public void setOnConnect(Runnable onConnect) {
		this.onConnect = onConnect;
	}
	
	@Override
	public synchronized void disconnect(Throwable th) {
		if(channel != null) {
			channel.close();
			channel = null;
		}
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return channel.remoteAddress();
	}
	
	public synchronized boolean connect(NettyPacketReadHandler rh) throws InterruptedException {
		if(channel != null) {
			channel.close();
			channel = null;
		}
		Bootstrap b = new Bootstrap(); // (1)
		
        b.group(new NioEventLoopGroup()); // (2)
        b.channel(NioSocketChannel.class); // (3)
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            
        	@Override
            public void initChannel(SocketChannel ch) throws Exception {
            	channel = ch;
                ch.pipeline().addLast(rh);
            }
        });
        b.connect(host, port).await(5_000); // (5)
        if(isConnected())
        	onConnect.run();
        return isConnected();
	}
	
	public void putWrite(Packet packet) {
		putWrite(packet.toBytes());
	}
	
	@Override
	public void putWrite(ByteBuffer buffer) {
		ByteBuf buf = Unpooled.wrappedBuffer(buffer);
		channel.writeAndFlush(buf);
	}
	
	@Override
	public void putWrite(byte[] bytes) {
		ByteBuf buf = Unpooled.wrappedBuffer(bytes);
		channel.writeAndFlush(buf);
	}

	@Override
	public boolean isConnected() {
		return channel != null && channel.isActive();
	}
}
