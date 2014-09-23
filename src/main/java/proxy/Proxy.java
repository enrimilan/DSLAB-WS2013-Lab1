package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Shell;
import util.ChecksumUtils;
import util.Config;
import message.Response;
import message.request.BuyRequest;
import message.request.DownloadTicketRequest;
import message.request.InfoRequest;
import message.request.ListRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.BuyResponse;
import message.response.CreditsResponse;
import message.response.DownloadTicketResponse;
import message.response.InfoResponse;
import message.response.ListResponse;
import message.response.LoginResponse;
import message.response.MessageResponse;
import model.DownloadTicket;
import model.FileServerInfo;
import model.UserInfo;

public class Proxy implements IProxy {

	private int tcpPort;
	private int udpPort;
	private int fileserverTimeout;
	private int fileserverCheckperiod;
	private ArrayList<UserInfo> userInfos;
	private Shell shell;
	private TCPListener tcpListener;
	private UDPListener udpListener;
	private FileServerAliveChecker fileServerAliveChecker;
	private ExecutorService threadPool;
	private ArrayList<FileServerInfo> fileServerInfos;
	private int currentUserPosition = -1;
	private ArrayList<UploadRequest> requestsQueue;

	public Proxy(Config config, Shell shell){
		tcpPort = config.getInt("tcp.port");
		udpPort = config.getInt("udp.port");
		fileserverTimeout = config.getInt("fileserver.timeout");
		fileserverCheckperiod = config.getInt("fileserver.checkPeriod");
		ArrayList<String> usernames = retrieveUsernames();
		retrieveUserInfo(usernames);
		this.shell = shell;
		fileServerInfos = new ArrayList<FileServerInfo>();
		threadPool = Executors.newCachedThreadPool();
		requestsQueue = new ArrayList<UploadRequest>();

	}

	public Shell getShell(){
		return this.shell;
	}

	public ArrayList<FileServerInfo> getFileServerInfos(){
		return this.fileServerInfos;
	}

	public ArrayList<UserInfo> getUserInfos(){
		return this.userInfos;
	}

	public TCPListener getTCPListener(){
		return this.tcpListener;
	}

	public UDPListener getUDPListener(){
		return this.udpListener;
	}

	public FileServerAliveChecker getFileServerAliveChecker(){
		return this.fileServerAliveChecker;
	}
	
	public ExecutorService getThreadPool(){
		return this.threadPool;
	}

	private ArrayList<String> retrieveUsernames(){
		ArrayList<String> usernames = new ArrayList<String>();

		Scanner s = null;

		try {
			URL url = ClassLoader.getSystemClassLoader().getResource("user.properties");

			s = new Scanner(new BufferedReader(new FileReader(new File(url.toURI()))));

			while (s.hasNextLine()) {
				String line = s.nextLine();
				if(line.indexOf(".credits")!= -1){
					usernames.add(line.substring(0, line.indexOf(".credits")));
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File doesn't exist!");
		} catch (URISyntaxException e) {
			System.out.println("Please check the syntax of your url");
		} finally {
			if (s != null) {
				s.close();
			}
		}
		return usernames;
	}

	private void retrieveUserInfo(ArrayList<String> usernames){
		userInfos = new ArrayList<UserInfo>();
		Config userConfig = new Config("user");
		for(String username : usernames){
			userInfos.add(new UserInfo(username, userConfig.getString(username+".password"),userConfig.getInt(username+".credits"), false));
		}
	}

	public void startProxy(){
		startShell();
		startTCPListener();
		startUDPListener();
		startCheckingifFileServersAreAlive();
	}

	private void startShell(){
		//start receiving commands from stdin
		threadPool.execute(shell);
	}

	private void startTCPListener(){
		//start listening to tcp requests from clients
		tcpListener = new TCPListener(tcpPort, this);
		threadPool.execute(tcpListener);
	}

	private void startUDPListener(){
		//start listening to udp requests from fileservers
		udpListener = new UDPListener(udpPort,this);
		threadPool.execute(udpListener);
	}

	private void startCheckingifFileServersAreAlive() {
		//start checking if fileservers are still alive
		fileServerAliveChecker = new FileServerAliveChecker(fileserverTimeout,fileserverCheckperiod, fileServerInfos);
		threadPool.execute(fileServerAliveChecker);
	}

	public void addFileServerInfo(FileServerInfo fileServerInfo) throws IOException {

		for(int i = 0; i<fileServerInfos.size(); i++){
			FileServerInfo fileServer = fileServerInfos.get(i);
			if(fileServer.getAddress().getHostAddress().equals(fileServerInfo.getAddress().getHostAddress()) && fileServer.getPort() == fileServerInfo.getPort()){
				fileServerInfos.set(i, new FileServerInfo(fileServerInfo.getAddress(), fileServerInfo.getPort(), fileServer.getUsage(), true, fileServerInfo.getLastSeen()));
				return;
			}
		}
		fileServerInfos.add(fileServerInfo);
		if(fileServerInfos.size()>1){
			synchronizeFileServer(fileServerInfo);
		}
	}

	private void synchronizeFileServer(FileServerInfo fileServerInfo) throws IOException{
		Socket socket = null;
		ObjectOutputStream objectOutputStream  = null;
		ObjectInputStream objectInputStream = null;
		for(UploadRequest request : requestsQueue){
			socket = new Socket(fileServerInfo.getAddress(),fileServerInfo.getPort());
			objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			objectInputStream = new ObjectInputStream(socket.getInputStream());
			objectOutputStream.writeObject(request);
			try {
				objectInputStream.readObject();
				objectOutputStream.close();
				objectInputStream.close();
				socket.close();
				System.out.println("got response");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		if(objectOutputStream != null){
			objectOutputStream.close();
		}
		if(objectInputStream != null){
			objectInputStream.close();
		}
		if(socket !=null){
			socket.close();
		}
	}

	public synchronized void setCurrentUserPosition(int currentUserPosition){
		this.currentUserPosition = currentUserPosition;
	}

	public synchronized int getCurrentUserPosition(){
		return currentUserPosition;
	}

	public synchronized UserInfo getCurrentUser(){
		return userInfos.get(currentUserPosition);
	}

	private FileServerInfo chooseBestFileServer(){
		int min = -1;
		FileServerInfo bestFileServer = null;
		for(FileServerInfo fileServerInfo : fileServerInfos){
			if(min == -1 && fileServerInfo.isOnline()){
				bestFileServer = fileServerInfo;
				min = (int) fileServerInfo.getUsage();
			}
			else if(bestFileServer !=null && fileServerInfo.getUsage()<bestFileServer.getUsage() && fileServerInfo.isOnline()){
				bestFileServer = fileServerInfo;
				min = (int) fileServerInfo.getUsage();
			}
		}

		return bestFileServer;
	}

	/***********************************************************************************************
	 ******************************** HANDLE REQUESTS FROM CLIENTS *********************************
	 ***********************************************************************************************/

	@Override
	public synchronized LoginResponse login(LoginRequest request) throws IOException {
		for(int i = 0; i<userInfos.size();i++){
			UserInfo userInfo = userInfos.get(i);
			if(userInfo.getName().equals(request.getUsername()) && userInfo.getPassword().equals(request.getPassword()) && !userInfo.isOnline()){
				UserInfo newUserInfo = new UserInfo(userInfo.getName(),userInfo.getPassword(),userInfo.getCredits(),true);
				userInfos.set(i, newUserInfo);
				setCurrentUserPosition(i);
				return new LoginResponse(LoginResponse.Type.SUCCESS);
			}
		}
		return new LoginResponse(LoginResponse.Type.WRONG_CREDENTIALS);
	}

	@Override
	public synchronized Response credits() throws IOException {
		if(getCurrentUserPosition() != -1){
			return new CreditsResponse(getCurrentUser().getCredits());
		}
		return new MessageResponse("You are not logged in!");
	}

	@Override
	public synchronized Response buy(BuyRequest credits) throws IOException {
		if(getCurrentUserPosition() != -1){
			UserInfo newUserInfo = new UserInfo(getCurrentUser().getName(),getCurrentUser().getPassword(),getCurrentUser().getCredits()+credits.getCredits(),true);
			userInfos.set(getCurrentUserPosition(), newUserInfo);
			return new BuyResponse(getCurrentUser().getCredits());
		}
		return new MessageResponse("You are not logged in!");
	}

	@Override
	public Response list() throws IOException {
		if(getCurrentUserPosition() != -1){
			HashSet<String> list = new HashSet<String>();
			ObjectOutputStream objectOutputStream = null;
			ObjectInputStream  objectInputStream = null;
			Socket socket = null;
			for (FileServerInfo fileServerInfo : fileServerInfos){
				if(fileServerInfo.isOnline()){
					socket = new Socket(fileServerInfo.getAddress(),fileServerInfo.getPort());
					objectOutputStream= new ObjectOutputStream(socket.getOutputStream());
					objectInputStream = new ObjectInputStream(socket.getInputStream());
					objectOutputStream.writeObject(new ListRequest());
					try {
						ListResponse response = (ListResponse) objectInputStream.readObject();
						list.addAll(response.getFileNames());
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}

				}
			}
			if(objectOutputStream != null){
				objectOutputStream.close();
			}
			if(objectInputStream != null){
				objectInputStream.close();
			}
			if(socket !=null){
				socket.close();
			}
			return new ListResponse(list);
		}
		return new MessageResponse("You are not logged in!");
	}

	@Override
	public synchronized Response download(DownloadTicketRequest request) throws IOException {
		if(getCurrentUserPosition() != -1){
			FileServerInfo fileServerInfo = chooseBestFileServer();
			if(fileServerInfo == null){
				return new MessageResponse("Can't download " + request.getFilename() + " because there is no fileserver available");
			}
			Socket socket = new Socket(fileServerInfo.getAddress(),fileServerInfo.getPort());
			ObjectOutputStream objectOutputStream= new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
			objectOutputStream.writeObject(new InfoRequest(request.getFilename()));
			Response infoResponse = null;
			try {
				infoResponse = (Response) objectInputStream.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
				if(objectOutputStream != null){
					objectOutputStream.close();
				}
				if(objectInputStream != null){
					objectInputStream.close();
				}
				if(socket !=null){
					socket.close();
				}

				if(infoResponse instanceof MessageResponse){
					return new MessageResponse("File not found.");
				}
				if(((InfoResponse) infoResponse).getSize()>getCurrentUser().getCredits()){
					return new MessageResponse("You don't have enough credits.");
				}
				buy(new BuyRequest(-((InfoResponse) infoResponse).getSize()));
				String checksum = ChecksumUtils.generateChecksum(getCurrentUser().getName(), request.getFilename(), 0, ((InfoResponse) infoResponse).getSize());
				DownloadTicket ticket = new DownloadTicket(getCurrentUser().getName(), request.getFilename(), checksum, fileServerInfo.getAddress(), fileServerInfo.getPort());
				fileServerInfo.increaseUsage(((InfoResponse) infoResponse).getSize());
				return new DownloadTicketResponse(ticket);
			
		}
		return new MessageResponse("You are not logged in!");
	}

	@Override
	public synchronized MessageResponse upload(UploadRequest request) throws IOException {
		requestsQueue.add(request);
		if(getCurrentUserPosition() != -1){
			ObjectOutputStream objectOutputStream = null;
			ObjectInputStream  objectInputStream = null;
			Socket socket = null;
			MessageResponse response = new MessageResponse("Can't upload because there is no fileserver available");
			for (FileServerInfo fileServerInfo : fileServerInfos){
				if(fileServerInfo.isOnline()){
					socket = new Socket(fileServerInfo.getAddress(),fileServerInfo.getPort());
					objectOutputStream= new ObjectOutputStream(socket.getOutputStream());
					objectInputStream = new ObjectInputStream(socket.getInputStream());
					objectOutputStream.writeObject(request);

					try {
						MessageResponse messageResponse = (MessageResponse) objectInputStream.readObject();
						BuyResponse buyResponse = (BuyResponse) buy(new BuyRequest(2*(new String(request.getContent())).length()));
						response = new MessageResponse(messageResponse.toString()+"\n"+buyResponse.toString());
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
				else{
					response = new MessageResponse("One or more fileservers seem to be offline, please try again later.");
					break;
				}
			}
			if(objectOutputStream != null){
				objectOutputStream.close();
			}
			if(objectInputStream != null){
				objectInputStream.close();
			}
			if(socket !=null){
				socket.close();
			}

			return response;

		}
		return new MessageResponse("You are not logged in!");
	}

	@Override
	public synchronized MessageResponse logout() throws IOException {
		if(getCurrentUserPosition() != -1){
			UserInfo newUserInfo = new UserInfo(getCurrentUser().getName(),getCurrentUser().getPassword(),getCurrentUser().getCredits(),false);
			userInfos.set(getCurrentUserPosition(), newUserInfo);
			return new MessageResponse("Successfully logged out.");
		}
		return new MessageResponse("You are not logged in!");
	}

}
