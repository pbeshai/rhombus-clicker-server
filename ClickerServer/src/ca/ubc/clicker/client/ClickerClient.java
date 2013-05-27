package ca.ubc.clicker.client;

import java.io.IOException;
import java.net.Socket;

import ca.ubc.clicker.server.ClickerServer;


/**
 * A object for handling client connections to the server. They receive output
 * from the clicker base station. They can send input to start or stop voting
 * via messages:
 * 
 *   vote start
 *   vote end
 *   
 * Two threads are used for handling this: one for output, one for input.
 * 
 * @author pbeshai
 *
 */
public class ClickerClient {
	private static int clientId = 1;
	
	boolean alive = true;
	private Socket clientSocket; 
	private int id;
	
	private ClientOutputThread output;
	private ClientInputThread input;
	
	private ClickerServer server;
	
	public ClickerClient(Socket clientSocket, ClickerServer server) {
		this.id = clientId++;
		System.out.println("Client "+id+" connected.");
		
		this.clientSocket = clientSocket;
		this.server = server;
		
		// create output thread
		try {
			this.output = new ClientOutputThread(id, clientSocket.getOutputStream());
		} catch (IOException e) {
			System.err.println("Error getting output stream for client "+id);
		}
		
		// create input thread
		try {
			this.input = new ClientInputThread(id, clientSocket.getInputStream(), this);
		} catch (IOException e) {
			System.err.println("Error getting input stream for client "+id);
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
				System.out.println("Client "+ id + " finished.");
			} catch (IOException e) {
				System.err.println("Error closing socket for client " + id);
			}
		}
		
		return alive;
	}
	
	// sends output from server to client
	public void output(String message) {
		if (!checkStatus()) { // update status and if no longer alive, abort
			return;
		}
	
		try {
			output.getMessageQueue().add(message);
		} catch (IllegalStateException e) {
			System.err.println("Warning: no space left in queue for " + output);
		}
	}
	
	// sends input from client to server
	public void input(String message) {
		server.input(message, this);
	}
	
	
}