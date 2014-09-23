package proxy;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import model.FileServerInfo;

public class UDPListener implements Runnable {
	
	private int udpPort;
	private Proxy proxy;
	private DatagramSocket socket = null;
	private boolean listening = true;
	
	public UDPListener(int udpPort, Proxy proxy){
		this.udpPort = udpPort;
		this.proxy = proxy;
		try {
			socket = new DatagramSocket(udpPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void stopListening(){
		listening = false;
		socket.close();
	}
	
	@Override
	public void run() {
		while(listening){
			byte[] buf = new byte[5];
			DatagramPacket request = new DatagramPacket(buf, buf.length); 
			//receive the request from the fileserver
			try {
				socket.receive(request);
				String tcpPort = new String( request.getData()).trim();
				
				FileServerInfo fileServerInfo = new FileServerInfo(request.getAddress(), Integer.valueOf(tcpPort), 0, true, System.currentTimeMillis());
				proxy.addFileServerInfo(fileServerInfo);
			} 
			catch (SocketException e) {
			}catch (IOException e) {
				e.printStackTrace();
			}

			
		}
		

	}

}
