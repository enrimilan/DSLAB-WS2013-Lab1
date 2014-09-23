package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;

import util.Config;
import cli.Command;
import cli.Shell;
import message.Request;
import message.Response;
import message.request.BuyRequest;
import message.request.CreditsRequest;
import message.request.DownloadFileRequest;
import message.request.DownloadTicketRequest;
import message.request.ExitRequest;
import message.request.ListRequest;
import message.request.LoginRequest;
import message.request.LogoutRequest;
import message.request.UploadRequest;
import message.response.DownloadFileResponse;
import message.response.DownloadTicketResponse;
import message.response.MessageResponse;

public class ClientCli implements IClientCli {

	private String downloadDir;
	private String proxyHost;
	private int proxyTcpPort;
	private Shell shell;
	private Socket socket;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	private HashMap<String, Integer> files;
	private Thread shellThread;

	public ClientCli(Config config, Shell shell){
		downloadDir = config.getString("download.dir");
		proxyHost = config.getString("proxy.host");
		proxyTcpPort = config.getInt("proxy.tcp.port");
		this.shell = shell;
		readFiles();
	}

	private void readFiles(){
		files = new HashMap<String,Integer>();
		File path = new File(downloadDir);
		File[] filesArray = path.listFiles();
		for(int i=0; i<filesArray.length;i++){
			files.put(filesArray[i].getName(), 0);
		}
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
		shellThread = new Thread(shell);
		shellThread.start();
	}

	private Response sendRequest(Request request) throws IOException{
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		inputStream = new ObjectInputStream(socket.getInputStream());
		outputStream.writeObject(request);
		Response response;
		try {
			response = (Response) inputStream.readObject();

		} catch (ClassNotFoundException e) {
			return new MessageResponse("Something went wrong, the class does not exist.");
		}
		return response;

	}

	/***********************************************************************************************
	 *************************** HANDLE REQUESTS FROM STANDARD INPUT *******************************
	 ***********************************************************************************************/

	@Override
	@Command(value="login")
	public Response login(String username, String password) throws IOException{
		return sendRequest(new LoginRequest(username, password));
	}

	@Override
	@Command(value="credits")
	public Response credits() throws IOException{
		return sendRequest(new CreditsRequest());
	}

	@Override
	@Command(value="buy")
	public Response buy(long credits) throws IOException{
		return sendRequest(new BuyRequest(credits));
	}

	@Override
	@Command(value="list")
	public Response list() throws IOException {
		return sendRequest(new ListRequest());
	}

	@Override
	@Command(value="download")
	public Response download(String filename) throws IOException {
		Response response = sendRequest(new DownloadTicketRequest(filename));
		if(response instanceof MessageResponse){
			return response;
		}
		Socket socket = new Socket(((DownloadTicketResponse)response).getTicket().getAddress(),((DownloadTicketResponse)response).getTicket().getPort());
		ObjectOutputStream objectOutputStream= new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
		objectOutputStream.writeObject(new DownloadFileRequest(((DownloadTicketResponse)response).getTicket()));
		try {
			response = (Response) objectInputStream.readObject();
			if(objectOutputStream != null){
				objectOutputStream.close();
			}
			if(objectInputStream != null){
				objectInputStream.close();
			}
			if(socket !=null){
				socket.close();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if(response instanceof MessageResponse){
			return response;
		}
		else{
			File file = new File(downloadDir + "/" + ((DownloadFileResponse)response).getTicket().getFilename());
			file.createNewFile();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(((DownloadFileResponse)response).getContent());
			fileOutputStream.close();
			return response;
		}

	}

	@Override
	@Command(value="upload")
	public MessageResponse upload(String filename) throws IOException {
		readFiles();
		if(files.containsKey(filename)){
			File file = new File(downloadDir + "/" + filename);
			FileInputStream fileInputStream = new FileInputStream(file);
			byte[] data = new byte[ (int) file.length()];
			fileInputStream.read(data);
			fileInputStream.close();
			return (MessageResponse) sendRequest(new UploadRequest(filename, files.get(filename), data));
		}
		else{
			return new MessageResponse("File not found!");
		}
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
		outputStream = new ObjectOutputStream(socket.getOutputStream());
		try
		{
			outputStream.writeObject(new ExitRequest());
			if(outputStream != null){
				outputStream.close();
			}
		}
		catch(SocketException e){
		}
		if(inputStream != null){
			inputStream.close();
		}
		if(socket != null){
			socket.close();
		}
		System.in.close();
		shell.close();

		return new MessageResponse("Client shutdown");
	}

}
