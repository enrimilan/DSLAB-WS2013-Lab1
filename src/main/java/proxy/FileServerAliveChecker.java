package proxy;

import java.util.ArrayList;

import model.FileServerInfo;

public class FileServerAliveChecker implements Runnable {
	
	private int fileserverTimeout;
	private int fileserverCheckperiod;
	private ArrayList<FileServerInfo> fileServerInfos;
	private boolean checking = true;
	
	
	public FileServerAliveChecker(int fileserverTimeout,int fileserverCheckperiod, ArrayList<FileServerInfo> fileServerInfos) {
		this.fileserverTimeout = fileserverTimeout;
		this.fileserverCheckperiod = fileserverCheckperiod;
		this.fileServerInfos = fileServerInfos;
	}
	
	public void stopChecking(){
		checking = false;
	}
	
	@Override
	public void run() {
		while(checking){
			
			for(int i = 0; i<fileServerInfos.size(); i++){
				FileServerInfo fileServerInfo = fileServerInfos.get(i);
				if(System.currentTimeMillis()-fileServerInfo.getLastSeen()>fileserverTimeout && fileServerInfo.isOnline()){
					fileServerInfos.set(i, new FileServerInfo(fileServerInfo.getAddress(), fileServerInfo.getPort(), fileServerInfo.getUsage(), false, fileServerInfo.getLastSeen()));
				}
			}
			
			try {
				Thread.sleep(fileserverCheckperiod);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
