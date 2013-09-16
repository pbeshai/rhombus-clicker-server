package ca.ubc.clicker.client;

import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.ubc.clicker.server.io.IOServer;


/**
 * A object for handling client connections to the server. They receive output
 * from the clicker base station. They can send input to enable or disable voting
 * via messages:
 * 
 *   enable choices
 *   disable choices
 *   
 * Two threads are used for handling this: one for output, one for input.
 * 
 * @author pbeshai
 *
 */
public class ClickerClient {
	private static Logger log = LogManager.getLogger();
	
	private static int clientId = 1;
	
	boolean alive = true;
	private Socket clientSocket; 
	private int id;
	
	private ClientOutputThread output;
	private ClientInputThread input;
	
	private IOServer server;
	
	public ClickerClient(Socket clientSocket, IOServer server) {
		this.id = clientId++;
		log.info("Client "+id+" connected.");
		
		this.clientSocket = clientSocket;
		this.server = server;
		
		// create output thread
		try {
			this.output = new ClientOutputThread(id, clientSocket.getOutputStream());
		} catch (IOException e) {
			log.error("Error getting output stream for client "+id);
		}
		
		// create input thread
		try {
			this.input = new ClientInputThread(id, clientSocket.getInputStream(), this);
		} catch (IOException e) {
			log.error("Error getting input stream for client "+id);
		}
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	private boolean checkStatus() {
		if (!alive) // already not alive 
			return false; 
		
		alive = output.isAlive() && input.isAlive();
		if (!alive) { // just ended if either thread is done
			try {
				clientSocket.close();
				log.info("Client "+ id + " disconnected.");
			} catch (IOException e) {
				log.error("Error closing socket for client " + id);
			}
		}
		
		return alive;
	}
	
	public String toString() {
		return "client " + id;
	}
	
	// sends output from server to client
	public void output(String message) {
		if (!checkStatus()) { // update status and if no longer alive, abort
			return;
		}
	
		try {
			output.getMessageQueue().add(message);
		} catch (IllegalStateException e) {
			log.error("Warning: no space left in queue for " + output);
		}
	}
	
	// sends input from client to server
	public void input(String message) {
		server.input(message, this);
	}
}