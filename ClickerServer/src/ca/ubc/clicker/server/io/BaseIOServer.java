package ca.ubc.clicker.server.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.ubc.clicker.client.ClickerClient;
import ca.ubc.clicker.server.filters.Filter;

public class BaseIOServer implements IOServer {
	private static Logger log = LogManager.getLogger();
	
	private int serverPort;
	private List<ClickerClient> clients;
	private IOServer composedServer = null; // workaround since we can't do mixins :(
	private List<Filter> filters;
	
	public BaseIOServer(int serverPort) {
		this.serverPort = serverPort;
		this.clients = new LinkedList<ClickerClient>();
		this.filters = new LinkedList<Filter>();
	}
	
	public BaseIOServer(int serverPort, IOServer composedServer) {
		this(serverPort);
		this.composedServer = composedServer;
	}
	
	public void init() throws IOException {
		loadFilters();
	}

	@Override
	public void run() throws IOException, InterruptedException {
		init();

		ServerSocket serverSocket = null;
		Socket clientSocket = null;

		// bind the server to the right port
		if (serverPort >= 0) {
			try {
				serverSocket = new ServerSocket(serverPort);
				log.info("Successfully listening on port " + serverPort);

				// accept client connections
				try {
					while (true) {
						clientSocket = serverSocket.accept();
						// composed server will receive the 'input' calls from ClickerClient.
						ClickerClient client = new ClickerClient(clientSocket, this);
						clients.add(client);
					}
				} catch (IOException e) {
					log.warn("Server socket accept failed on port: " + serverPort);
					System.exit(-1);
				}

				serverSocket.close();

			} catch (IOException e) {
				log.warn("Could not listen on port: " + serverPort);
			}
		}

		log.warn("Failed to open socket on port " + serverPort + "; running locally");

		while (true) {
			Thread.sleep(300);
		}
	}

	public void input(String message) {
		if (composedServer != null) {
			message = filterInput(message);
			
			if (message == null) { // abort if message is empty
				return;
			}
			
			composedServer.input(message);
		} else {
			input(message, null);
		}
	}

	// should be overridden if not composed
	public void input(String message, ClickerClient client) {
		message = filterInput(message);
		
		if (message == null) { // abort if message is empty
			return; 
		}
		
		if (composedServer != null) {
			composedServer.input(message, client);
		} else {
			log.info("INPUT: "+message+", client: "+client);
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
	
	// can be overridden by subclasses
	protected String processOutput(String message) {
		return message;
	}
	
	// sends output to all the clients if client == null, otherwise just to client
	public void output(String message, ClickerClient client) {
		pruneClients();
		
		message = processOutput(message);
		
		// filter output
		message = filterOutput(message);
		
		if (message == null) { // abort if the message is empty
			return;
		}
		
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
	
	protected String filterOutput(String message) {
		for (Filter filter : filters) {
			message = filter.output(message);
			if (message == null) return null;
		}
		return message;
	}
	
	protected String filterInput(String message) {
		for (Filter filter : filters) {
			message = filter.input(message);
			if (message == null) return null;
		}
		return message;
	}
	
	public void initializeFilter(Filter filter) {
		if (composedServer != null) {
			composedServer.initializeFilter(filter);
		}
	}
	
	protected void loadFilters() {
		log.info("Loading filters...");
		ServiceLoader<Filter> filterLoader = ServiceLoader.load(Filter.class);
		for (Filter filter : filterLoader) {
			log.info("  -> " + filter.getClass().getSimpleName());
			initializeFilter(filter);
			filters.add(filter);
		}
	}
}
