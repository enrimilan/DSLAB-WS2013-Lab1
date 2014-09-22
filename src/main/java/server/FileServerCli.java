package server;

import java.io.IOException;

import cli.Command;
import message.Request;
import message.Response;
import message.request.InfoRequest;
import message.response.MessageResponse;

public class FileServerCli implements IFileServerCli {
	
	private FileServer fileServer;
	
	public FileServerCli(FileServer fileServer){
		this.fileServer = fileServer;
	}
	
	@Override
	@Command(value="exit")
	public MessageResponse exit() throws IOException {
		//TODO 
		return null;
	}

}
