package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Shell;
import util.ChecksumUtils;
import util.Config;
import message.Response;
import message.request.DownloadFileRequest;
import message.request.InfoRequest;
import message.request.UploadRequest;
import message.response.DownloadFileResponse;
import message.response.InfoResponse;
import message.response.ListResponse;
import message.response.MessageResponse;

public class FileServer implements IFileServer {

	private String fileserverDir;
	private int tcpPort;
	private String proxyHost;
	private int proxyUdpPort;
	private int fileserverAlive;
	private Shell shell;
	private TCPListener tcpListener;
	private UDPPacketSender udpPacketSender;
	private ExecutorService threadPool;
	private File[] files;

	public FileServer(Config config, Shell shell) {
		fileserverDir =config.getString("fileserver.dir");
		tcpPort = config.getInt("tcp.port");
		proxyHost = config.getString("proxy.host");
		proxyUdpPort = config.getInt("proxy.udp.port");
		fileserverAlive = config.getInt("fileserver.alive");
		this.shell = shell;
		threadPool = Executors.newCachedThreadPool(); //start the thread pool
	}

	public Shell getShell(){
		return this.shell;
	}
	
	public TCPListener getTCPListener(){
		return this.tcpListener;
	}
	
	public UDPPacketSender getUDPPacketSender(){
		return this.udpPacketSender;
	}
	
	public ExecutorService getThreadPool(){
		return this.threadPool;
	}

	public void startFileServer(){
		readFiles();
		startShell();
		startTcpListener();
		startSendingUdpPackets();
	}

	private void readFiles() {
		File path = new File(fileserverDir);
		files = path.listFiles();
	}

	private void startShell(){
		//start the shell thread for stdin
		threadPool.execute(shell); 
	}

	private void startTcpListener(){
		//start listening to tcp requests from clients and proxy
		tcpListener = new TCPListener(tcpPort,this);
		threadPool.execute(tcpListener);
	}

	private void startSendingUdpPackets() {
		//start sending udp alive packets to the proxy
		udpPacketSender = new UDPPacketSender(proxyUdpPort, tcpPort, proxyHost, fileserverAlive);
		threadPool.execute(udpPacketSender);
	}

	/***********************************************************************************************
	 ******************************** HANDLE REQUESTS FROM THE PROXY *******************************
	 ***********************************************************************************************/

	@Override
	public Response list() throws IOException {
		readFiles();
		Set<String> fileNames = new HashSet<String>();
		for (File file : files){
			fileNames.add(file.getName());
		}
		return new ListResponse(fileNames);
	}

	@Override
	public Response download(DownloadFileRequest request) throws IOException {
		readFiles();
		String user = request.getTicket().getUsername();
		String filename = request.getTicket().getFilename();
		File file = new File(fileserverDir + "/" + filename);
		String checksum = request.getTicket().getChecksum();
		if (ChecksumUtils.verifyChecksum(user, file, 0, checksum)){
			byte[] data = new byte[(int) file.length()];
			FileInputStream fileInputStream = new FileInputStream(file);
			fileInputStream.read(data);
			fileInputStream.close();

			return new DownloadFileResponse(request.getTicket(), data);
		}
		return new MessageResponse("Failed to verify checksum.");
	}

	@Override
	public Response info(InfoRequest request) throws IOException {
		readFiles();
		for(int i = 0; i<files.length; i++){
			if(files[i].getName().equals(request.getFilename())){
				return new InfoResponse(request.getFilename(),files[i].length());
			}
		}
		return new MessageResponse("File does not exist!");
	}

	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		File file = new File(fileserverDir + "/" + request.getFilename());
		file.createNewFile();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.write(request.getContent());
		fileOutputStream.close();
		readFiles();
		return new MessageResponse(request.getFilename() + " uploaded successfully to fileserver.");
	}

}
