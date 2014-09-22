package server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cli.Shell;
import util.Config;
import message.Response;
import message.request.DownloadFileRequest;
import message.request.InfoRequest;
import message.request.UploadRequest;
import message.request.VersionRequest;
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
	private ExecutorService threadPool;
	private File[] files;


	public FileServer(Config config, Shell shell) {
		fileserverDir =config.getString("fileserver.dir");
		tcpPort = config.getInt("tcp.port");
		proxyHost = config.getString("proxy.host");
		proxyUdpPort = config.getInt("proxy.udp.port");
		fileserverAlive = config.getInt("fileserver.alive");
		
		File path = new File(fileserverDir);
		files = path.listFiles();
		
		this.shell = shell;
		threadPool = Executors.newCachedThreadPool(); //start the thread pool
	}

	public void startFileServer(){
		startShell();
		startTcpListener();
		startSendingUdpPackets();
	}

	private void startSendingUdpPackets() {
		threadPool.execute(new UDPPacketSender(proxyUdpPort, tcpPort, proxyHost, fileserverAlive));
		
	}

	private void startShell(){
		threadPool.execute(shell); //start the shell thread for stdin
	}

	private void startTcpListener(){
		threadPool.execute(new TCPListener(tcpPort));
	}

	@Override
	public Response list() throws IOException {
		 Set<String> fileNames = new HashSet<String>();
		for (File file : files){
			fileNames.add(file.getName());
		}
		return new ListResponse(fileNames);
	}

	@Override
	public Response download(DownloadFileRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response info(InfoRequest request) throws IOException {
		for(int i = 0; i<files.length; i++){
			if(files[i].getName().equals(request.getFilename())){
				return new InfoResponse(request.getFilename(),files[i].length());
			}
		}
		return new MessageResponse("File does not exist!");
	}

	@Override
	public Response version(VersionRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse upload(UploadRequest request) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
