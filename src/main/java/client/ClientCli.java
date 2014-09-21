package client;

import java.io.IOException;

import cli.Command;
import message.Response;
import message.response.LoginResponse;
import message.response.MessageResponse;

public class ClientCli implements IClientCli {
	
	private Client client;
	
	public ClientCli(Client client){
		this.client = client;
	}
	
	@Override
	@Command(value="login")
	public Response login(String username, String password)
			throws IOException {
		
		return client.login(username, password);
	}

	@Override
	@Command(value="credits")
	public Response credits() throws IOException {
		return client.credits();
	}

	@Override
	@Command(value="buy")
	public Response buy(long credits) throws IOException {
		return client.buy(credits);
	}

	@Override
	public Response list() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response download(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageResponse upload(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Command(value="logout")
	public MessageResponse logout() throws IOException {
		return client.logout();
	}

	@Override
	@Command(value="exit")
	public MessageResponse exit() throws IOException {
		return client.exit();
	}

}
