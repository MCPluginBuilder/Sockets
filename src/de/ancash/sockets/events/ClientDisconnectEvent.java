package de.ancash.sockets.events;

import de.ancash.libs.org.bukkit.event.Event;
import de.ancash.libs.org.bukkit.event.HandlerList;
import de.ancash.sockets.io.ITCPClient;

public class ClientDisconnectEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	private final ITCPClient client;
	private final Throwable th;

	public ClientDisconnectEvent(ITCPClient client, Throwable th) {
		this.client = client;
		this.th = th;
	}

	public Throwable getThrowable() {
		return th;
	}
	
	public ITCPClient getClient() {
		return client;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}