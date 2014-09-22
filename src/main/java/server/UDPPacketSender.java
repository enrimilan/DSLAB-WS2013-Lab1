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
	private boolean sending = true;

	public UDPPacketSender(int proxyUdpPort, int tcpPort, String proxyHost, int fileserverAlive){
		this.proxyHost = proxyHost;
		this.tcpPort = tcpPort;
		this.proxyUdpPort = proxyUdpPort;
		this.fileserverAlive = fileserverAlive;
	}

	@Override
	public void run() {
		try {
			DatagramSocket socket = new DatagramSocket();
			while(sending){
				String info = ""+tcpPort;
				byte[] buf = info.getBytes();
				DatagramPacket request = new DatagramPacket(buf, buf.length, InetAddress.getByName(proxyHost), proxyUdpPort);
				socket.send(request);
				Thread.sleep(fileserverAlive);
			}
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
