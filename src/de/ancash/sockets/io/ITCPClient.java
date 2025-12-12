package de.ancash.sockets.io;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface ITCPClient {

	public void putWrite(byte[] b);
	
	public void putWrite(ByteBuffer buffer);
	
	public boolean isConnected();
	
	public void disconnect(Throwable th);
	
	public SocketAddress getRemoteAddress();
}
