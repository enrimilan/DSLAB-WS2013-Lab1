package server;

import java.io.IOException;

import cli.Command;
import message.response.MessageResponse;

public class FileServerCli implements IFileServerCli {

	private FileServer fileServer;

	public FileServerCli(FileServer fileServer){
		this.fileServer = fileServer;
	}

	@Override
	@Command(value="exit")
	public MessageResponse exit() throws IOException {
		fileServer.getTCPListener().stopListening();
		fileServer.getUDPPacketSender().stopSendingPackets();
	    fileServer.getShell().close();
		System.in.close();
		fileServer.getThreadPool().shutdown();
		return new MessageResponse("File server shutdown");
	}

}
