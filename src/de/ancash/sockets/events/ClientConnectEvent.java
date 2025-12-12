package de.ancash.sockets.events;

import de.ancash.libs.org.bukkit.event.Event;
import de.ancash.libs.org.bukkit.event.HandlerList;
import de.ancash.sockets.io.ITCPClient;

public class ClientConnectEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	private final ITCPClient cl;

	public ClientConnectEvent(ITCPClient cl) {
		this.cl = cl;
	}

	public ITCPClient getClient() {
		return cl;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}