package proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Shell;
import util.Config;
import message.Response;
import message.request.BuyRequest;
import message.request.DownloadTicketRequest;
import message.request.LoginRequest;
import message.request.UploadRequest;
import message.response.FileServerInfoResponse;
import message.response.LoginResponse;
import message.response.MessageResponse;
import model.FileServerInfo;
import model.UserInfo;

public class Proxy implements IProxy {

	private int tcpPort;
	private int udpPort;
	private int fileserverTimeout;
	private int fileserverCheckperiod;
	private ArrayList<UserInfo> userInfos;
	private TCPListener tcpListener;
	private Shell shell;
	private ExecutorService threadPool;
	private UserInfo LoggedInUserInfo;
	private ArrayList<FileServerInfo> fileServerInfos;

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

	}

	public void setLoggedInUserInfo(UserInfo LoggedInUserInfo ){
		this.LoggedInUserInfo = LoggedInUserInfo;
	}

	public void startProxy(){
		startShell();
		startTCPListener();
		startUDPListener();
		startCheckingifFileServersAreAlive();
	}

	private void startCheckingifFileServersAreAlive() {
		threadPool.execute(new FileServerAliveChecker(fileserverTimeout,fileserverCheckperiod, fileServerInfos));
	}

	public void exit(){
		shell.close();
		threadPool.shutdown();
		try {
			System.in.close();
			if(tcpListener!=null){
				tcpListener.stopListening();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startTCPListener(){

		tcpListener = new TCPListener(tcpPort, this);
		threadPool.execute(tcpListener);
		//will start to listen to tcp requests
	}

	private void startUDPListener(){
		threadPool.execute(new UDPListener(udpPort,this));
	}

	private void startShell(){
		threadPool.execute(shell);
	}

	public ArrayList<UserInfo> getUserInfos() {
		return userInfos;
	}

	@Override
	public LoginResponse login(LoginRequest request) throws IOException {

		return null;
	}

	private void retrieveUserInfo(ArrayList<String> usernames){
		userInfos = new ArrayList<UserInfo>();
		Config userConfig = new Config("user");
		for(String username : usernames){
			userInfos.add(new UserInfo(username, userConfig.getString(username+".password"),userConfig.getInt(username+".credits"), false));
		}
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (s != null) {
				s.close();
			}
		}
		return usernames;

	}

	@Override
	public Response credits() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response buy(BuyRequest credits) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response list() throws IOException {
		return null;
	}

	public Response fileservers() throws IOException {
		return new FileServerInfoResponse(fileServerInfos);

	}

	@Override
	public Response download(DownloadTicketRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse logout() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void addFileServerInfo(FileServerInfo fileServerInfo) {

		for(int i = 0; i<fileServerInfos.size(); i++){
			FileServerInfo fileServer = fileServerInfos.get(i);
			if(fileServer.getAddress().getHostAddress().equals(fileServerInfo.getAddress().getHostAddress()) && fileServer.getPort() == fileServerInfo.getPort() && fileServer.getUsage() == fileServerInfo.getUsage()){
				fileServerInfos.set(i, new FileServerInfo(fileServerInfo.getAddress(), fileServerInfo.getPort(), fileServer.getUsage(), true, fileServerInfo.getLastSeen()));
				return;
			}
		}
		fileServerInfos.add(fileServerInfo);
	}

}
