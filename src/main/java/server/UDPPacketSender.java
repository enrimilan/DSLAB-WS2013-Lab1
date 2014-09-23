package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPPacketSender implements Runnable {

	private int proxyUdpPort;
	private int tcpPort;
	private String proxyHost;
	private int fileserverAlive;
	private DatagramSocket socket;
	private boolean sending = true;

	public UDPPacketSender(int proxyUdpPort, int tcpPort, String proxyHost, int fileserverAlive){
		this.proxyHost = proxyHost;
		this.tcpPort = tcpPort;
		this.proxyUdpPort = proxyUdpPort;
		this.fileserverAlive = fileserverAlive;
	}
	
	public void stopSendingPackets(){
		if(socket != null){
			socket.close();
		}
		sending = false;
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket();
			while(sending){
				String info = ""+tcpPort;
				byte[] buf = info.getBytes();
				DatagramPacket request = new DatagramPacket(buf, buf.length, InetAddress.getByName(proxyHost), proxyUdpPort);
				socket.send(request);
				Thread.sleep(fileserverAlive);
			}
			socket.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
