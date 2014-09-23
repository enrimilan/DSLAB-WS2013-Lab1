package model;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

/**
 * Contains information about a {@link server.IFileServer} and its state.
 */
public class FileServerInfo implements Serializable {
	private static final long serialVersionUID = 5230922478399546921L;

	private InetAddress address;
	private int port;
	private long usage;
	private boolean online;
	private long lastSeen;

	public FileServerInfo(InetAddress address, int port, long usage, boolean online, long lastSeen) {
		this.address = address;
		this.port = port;
		this.usage = usage;
		this.online = online;
		this.lastSeen = lastSeen;
	}

	@Override
	public String toString() {
		return String.format("%1$-15s %2$-5d %3$-7s %4$13d",
				getAddress().getHostAddress(), getPort(),
				isOnline() ? "online" : "offline", getUsage())+"     last packet sent: "+new Date(lastSeen);
	}

	public InetAddress getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public long getUsage() {
		return usage;
	}
	
	public void increaseUsage(long usage){
		this.usage = this.usage + usage;
	}

	public boolean isOnline() {
		return online;
	}
	public long getLastSeen(){
		return lastSeen;
	}
}
