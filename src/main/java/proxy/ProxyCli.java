package proxy;

import java.io.IOException;

import cli.Command;
import cli.Shell;
import message.Response;
import message.response.MessageResponse;
import message.response.UserInfoResponse;

public class ProxyCli implements IProxyCli {
	private Proxy proxy;
	
	public ProxyCli(Proxy proxy){
		this.proxy = proxy;
	}

	@Override
	public Response fileservers() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Command(value="users")
	public Response users() throws IOException { 
		return new UserInfoResponse(proxy.getUserInfos());
	}

	@Override
	@Command(value="exit")
	public MessageResponse exit() throws IOException {
		proxy.exit();
		return new MessageResponse("Shuting down the proxy.");
	}

}