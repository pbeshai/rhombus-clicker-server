package ca.ubc.clicker.server.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import ca.ubc.clicker.client.ClickerClient;

public class BaseIOServer implements IOServer {
	private int serverPort;
	private List<ClickerClient> clients;
	private IOServer composedServer = null; // workaround since we can't do mixins :(
		
	public BaseIOServer(int serverPort) {
		this.serverPort = serverPort;
		this.clients = new LinkedList<ClickerClient>();
	}
	
	public BaseIOServer(int serverPort, IOServer composedServer) {
		this(serverPort);
		this.composedServer = composedServer;
	}
	
	public void init() throws IOException { } 

	@Override
	public void run() throws IOException, InterruptedException {
		init();

		ServerSocket serverSocket = null;
		Socket clientSocket = null;

		// bind the server to the right port
		if (serverPort >= 0) {
			try {
				serverSocket = new ServerSocket(serverPort);
				System.out.println("Successfully listening on port " + serverPort);

				// accept client connections
				try {
					while (true) {
						clientSocket = serverSocket.accept();
						// composed server will receive the 'input' calls from ClickerClient.
						ClickerClient client = new ClickerClient(clientSocket, this);
						clients.add(client);
					}
				} catch (IOException e) {
					System.err.println("Server socket accept failed on port: " + serverPort);
					System.exit(-1);
				}

				serverSocket.close();

			} catch (IOException e) {
				System.err.println("ERROR: Could not listen on port: " + serverPort);
			}
		}

		System.out.println("Failed to open socket on port " + serverPort + "; running locally");

		while (true) {
			Thread.sleep(300);
		}
	}

	public void input(String message) {
		if (composedServer != null) {
			composedServer.input(message);
		} else {
			input(message, null);
		}
	}

	// should be overridden if not composed
	public void input(String message, ClickerClient client) {
		if (composedServer != null) {
			composedServer.input(message, client);
		} else {
			System.out.println("INPUT: "+message+", client: "+client);
		}
	}
	
	// removes dead clients
	private void pruneClients() {
		for (int i = 0; i < clients.size(); i++) {
			ClickerClient currClient = clients.get(i);
			// prune dead clients
			if(!currClient.isAlive()) {
				clients.remove(currClient);
				i--;
				continue;
			} 
		}
	}
	
	// sends output to all the clients
	@Override
	public void output(String message) {
		output(message, null);
	}
	
	// sends output to all the clients if client == null, otherwise just to client
	public void output(String message, ClickerClient client) {
		pruneClients();
		
		if (client == null) { // broadcast
			// send to all the clients
			for (int i = 0; i < clients.size(); i++) {
				// broadcast message to each client
				clients.get(i).output(message);
			}
		// don't broadcast
		} else if(client.isAlive()) {
			// send message to individual client
			client.output(message);
		}
	}
	
	@Override
	public int getNumClients() {
		return clients.size();
	}
}
