package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import util.Config;
import cli.Command;
import cli.Shell;
import message.Request;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.ExitRequest;
import message.request.LoginRequest;
import message.request.LogoutRequest;
import message.response.MessageResponse;

public class ClientCli implements IClientCli {

	private String downloadDir;
	private String proxyHost;
	private int proxyTcpPort;
	private Shell shell;
	private Socket socket;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;

	public ClientCli(Config config, Shell shell){
		downloadDir = config.getString("download.dir");
		proxyHost = config.getString("proxy.host");
		proxyTcpPort = config.getInt("proxy.tcp.port");
		this.shell = shell;
	}

	public void startClient(){
		connectToProxy();
		startShell();
	}

	private void connectToProxy() {
		try {
			socket = new Socket(proxyHost, proxyTcpPort);
		} catch (UnknownHostException e) {
			System.out.println("Unknown host!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startShell() {
		shell.run();
	}

	private Response sendRequest(Request request) throws IOException{
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		inputStream = new ObjectInputStream(socket.getInputStream());
		outputStream.writeObject(request);
		try {
			return (Response) inputStream.readObject();
		} catch (ClassNotFoundException e) {
			return new MessageResponse("Something went wrong, the class does not exist.");
		}

	}

	/***********************************************************************************************
	 *************************** HANDLE REQUESTS FROM STANDARD INPUT *******************************
	 ***********************************************************************************************/

	@Override
	@Command(value="login")
	public Response login(String username, String password) throws IOException{
		LoginRequest request = new LoginRequest(username, password);
		return sendRequest(request);
	}

	@Override
	@Command(value="credits")
	public Response credits() throws IOException{
		CreditsRequest request = new CreditsRequest();
		return sendRequest(request);
	}

	@Override
	@Command(value="buy")
	public Response buy(long credits) throws IOException{
		BuyRequest request = new BuyRequest(credits);
		return sendRequest(request);
	}

	@Override
	public Response list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response download(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse upload(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Command(value="logout")
	public MessageResponse logout() throws IOException{
		LogoutRequest request = new LogoutRequest();
		return (MessageResponse) sendRequest(request);
	}

	@Override
	@Command(value="exit")
	public MessageResponse exit() throws IOException{
		ExitRequest request = new ExitRequest();

		shell.close();
		System.in.close();
		if(inputStream !=null){
			inputStream.close();
		}
		if(outputStream !=null){
			outputStream.close();
		}
		socket.close();
		return (MessageResponse) sendRequest(request);
	}

}
