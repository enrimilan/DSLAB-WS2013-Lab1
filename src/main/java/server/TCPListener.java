package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import message.request.DownloadFileRequest;
import message.request.InfoRequest;
import message.request.ListRequest;
import message.request.UploadRequest;

public class TCPListener implements Runnable {

	private int tcpPort;
	private FileServer fileServer;
	private boolean active = true;
	private ServerSocket serverSocket;
	private Socket socket;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;

	public TCPListener(int tcpPort, FileServer fileServer){
		this.tcpPort = tcpPort;
		this.fileServer = fileServer;
	}

	public void stopListening(){
		active = false;
		try{
			if(outputStream != null){
				outputStream.close();
			}
			if(inputStream != null){
				inputStream.close();
			}
			if(socket !=null){
				socket.close();
			}
			if(serverSocket !=null){
				serverSocket.close();
			}
		}catch (IOException e){
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(tcpPort);
			while(active){
				socket = serverSocket.accept();
				inputStream = new ObjectInputStream(socket.getInputStream());
				outputStream = new ObjectOutputStream(socket.getOutputStream());
				Object readObject = inputStream.readObject();
				if(readObject instanceof ListRequest){
					outputStream.writeObject(fileServer.list());
				}
				if(readObject instanceof UploadRequest){
					outputStream.writeObject(fileServer.upload((UploadRequest) readObject));
				}
				if(readObject instanceof DownloadFileRequest){
					outputStream.writeObject(fileServer.download((DownloadFileRequest) readObject));
				}
				if(readObject instanceof InfoRequest){
					outputStream.writeObject(fileServer.info((InfoRequest) readObject));
				}
				inputStream = null;
				outputStream = null;
			}
		} catch (SocketException e) {
		} catch (IOException e) {
			e.printStackTrace();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}