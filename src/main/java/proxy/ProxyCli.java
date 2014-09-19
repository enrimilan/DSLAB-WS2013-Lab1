package proxy;

import java.io.IOException;

import message.Response;
import message.response.MessageResponse;

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
	public Response users() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}