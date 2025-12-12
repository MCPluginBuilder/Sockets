package de.ancash.sockets.async.impl.packet.client.netty;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import de.ancash.Sockets;
import de.ancash.sockets.async.impl.packet.netty.NettyPacketReadHandler;
import de.ancash.sockets.io.ITCPClient;
import de.ancash.sockets.netty.NettyClient;
import de.ancash.sockets.packet.Packet;
import de.ancash.sockets.packet.PacketCallback;

public class NettyPacketClient implements ITCPClient{


	private final Map<Long, PacketCallback> packetCallbacks = new ConcurrentHashMap<>();
	private final Map<Long, Packet> awaitResponses = new ConcurrentHashMap<>();
	private final AtomicReference<Long> lock = new AtomicReference<>(null);
	private final AtomicLong packetId = new AtomicLong();
	private final NettyClient nettyClient;
	private Consumer<Packet> onPacket;
	private Runnable onDisconnect;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		new NettyPacketClient("localhost", 25000, p -> System.out.println("p"), () -> System.out.println("c"), () -> System.out.println("d"));
	}

	public NettyPacketClient(String address, int port, Consumer<Packet> onPacket, Runnable onConnect, Runnable onDisconnect) {
		this.nettyClient = new NettyClient(address, port);
		this.onPacket = onPacket;
		this.onDisconnect = onDisconnect;
		nettyClient.setOnDisconnect(() -> onDisconnect());
		nettyClient.setOnConnect(onConnect);
	}
	
	public NettyPacketClient(NettyClient client, Consumer<Packet> onPacket, Runnable onDisconnect) {
		this.nettyClient = client;
		this.onDisconnect = onDisconnect;
		nettyClient.setOnDisconnect(() -> onDisconnect());
	}
	
	public void setOnPacket(Consumer<Packet> onPacket) {
		this.onPacket = onPacket;
	}
	
	public boolean connect() throws InterruptedException {
		return nettyClient.connect(new NettyPacketReadHandler(p -> onPacket(p), onDisconnect));
	}
	
	public void onPacket(Packet packet) {
		PacketCallback pc = null;
		Packet awake;
		pc = packetCallbacks.remove(packet.getTimeStamp());
		awake = awaitResponses.remove(packet.getTimeStamp());
		if (pc != null)
			pc.call(packet.getObject());
		if (awake != null)
			awake.awake(packet);
		onPacket.accept(packet);
	}
	
	@Override
	public void putWrite(byte[] b) {
		nettyClient.putWrite(b);
	}
	
	@Override
	public void putWrite(ByteBuffer buffer) {
		nettyClient.putWrite(buffer);
	}

	
	public final void write(Packet packet) {
		while (packetCallbacks.size() + awaitResponses.size() > 70 && (packet.hasPacketCallback() || packet.isAwaitingRespose())) {
			Sockets.sleepMillis(1);
		}
		packet.setLong(packetId.getAndIncrement());
		if (packet.hasPacketCallback()) {
			packetCallbacks.put(packet.getTimeStamp(), packet.getPacketCallback());
		}
		if (packet.isAwaitingRespose())
			awaitResponses.put(packet.getTimeStamp(), packet);
		nettyClient.putWrite(packet);
	}

	private synchronized void onDisconnect() {
		try {
			while (!lock.compareAndSet(null, Thread.currentThread().getId())
					&& !lock.compareAndSet(Thread.currentThread().getId(), Thread.currentThread().getId()))
				Sockets.sleepMillis(1);
			packetCallbacks.clear();
			awaitResponses.clear();
		} finally {
			lock.set(null);
		}
		onDisconnect.run();;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return nettyClient.getRemoteAddress();
	}
	
	@Override
	public void disconnect(Throwable th) {
		nettyClient.disconnect(th);
	}
	
	public boolean isConnected() {
		return nettyClient.isConnected();
	}
}