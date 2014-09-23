package proxy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadTicketRequest;
import message.request.ExitRequest;
import message.request.ListRequest;
import message.request.LoginRequest;
import message.request.LogoutRequest;
import message.request.UploadRequest;
import message.response.LoginResponse;

public class ClientHandler implements Runnable {
	private boolean active = true;
	private int LoggedInUserInfoPosition = -1;
	private Socket socket;
	private Proxy proxy;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;

	public ClientHandler(Socket socket, Proxy proxy){
		this.socket = socket;
		this.proxy = proxy;
	}

	public void handleExit(){
		try {
			proxy.setCurrentUserPosition(LoggedInUserInfoPosition);
			proxy.logout();
			active = false;
			socket.close();
			LoggedInUserInfoPosition = -1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void run() {
		try {
			while(active){
				inputStream = new ObjectInputStream(socket.getInputStream());
				outputStream = new ObjectOutputStream(socket.getOutputStream());
				Object readObject = inputStream.readObject();
				if (readObject instanceof LoginRequest) {
					LoginRequest loginRequest = (LoginRequest) readObject;
					LoginResponse loginResponse = proxy.login(loginRequest);
					if(loginResponse.getType().equals(LoginResponse.Type.SUCCESS)){
						LoggedInUserInfoPosition = proxy.getCurrentUserPosition();
					}
					outputStream.writeObject(loginResponse);
				}

				if(readObject instanceof CreditsRequest){
					proxy.setCurrentUserPosition(LoggedInUserInfoPosition);
					outputStream.writeObject(proxy.credits());
				}

				if(readObject instanceof BuyRequest){
					proxy.setCurrentUserPosition(LoggedInUserInfoPosition);
					outputStream.writeObject(proxy.buy((BuyRequest) readObject));
				}

				if(readObject instanceof LogoutRequest){
					proxy.setCurrentUserPosition(LoggedInUserInfoPosition);
					LoggedInUserInfoPosition = -1;
					outputStream.writeObject(proxy.logout());
				}
				if(readObject instanceof ListRequest){
					proxy.setCurrentUserPosition(LoggedInUserInfoPosition);
					outputStream.writeObject(proxy.list());
				}
				if(readObject instanceof UploadRequest){
					proxy.setCurrentUserPosition(LoggedInUserInfoPosition);
					outputStream.writeObject(proxy.upload((UploadRequest) readObject));
				}
				if(readObject instanceof DownloadTicketRequest){
					proxy.setCurrentUserPosition(LoggedInUserInfoPosition);
					outputStream.writeObject(proxy.download( (DownloadTicketRequest) readObject));
				}
				if(readObject instanceof ExitRequest){
					handleExit();
				}
			}
		}
		catch(SocketException e){
			handleExit();
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

}