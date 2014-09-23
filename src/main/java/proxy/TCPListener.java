package proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPListener implements Runnable {

	private int tcpPort;
	private Proxy proxy;
	private boolean listening = true;
	private ExecutorService threadPool;
	private ServerSocket serverSocket;
	private Socket socket;
	private ClientHandler c =null;
	private ArrayList<ClientHandler> handlers;

	public TCPListener(int tcpPort, Proxy proxy){
		this.tcpPort = tcpPort;
		this.proxy = proxy;
		threadPool = Executors.newCachedThreadPool();
		this.handlers = new ArrayList<ClientHandler>();
	}

	public void stopListening(){

		try {
			serverSocket.close();
			for(ClientHandler client : handlers){
				client.handleExit();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		listening = false;
		threadPool.shutdown();
	}

	@Override
	public void run() {

		try {
			serverSocket = new ServerSocket(tcpPort);
			while (listening) {

				socket = serverSocket.accept();
				threadPool.execute( c =new ClientHandler(socket, proxy));
				handlers.add(c);
			} 
		}
		catch(SocketException e){
			//stopListening();
		}
		catch (IOException e) {
			System.err.println("Could not listen on port " + tcpPort);
		}

	}

}
