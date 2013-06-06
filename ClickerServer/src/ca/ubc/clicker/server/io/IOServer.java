package ca.ubc.clicker.server.io;

import java.io.IOException;

import ca.ubc.clicker.client.ClickerClient;

public interface IOServer {

	// queue up input without specifying clicker client
	public abstract void input(String message);

	// public interface for others to queue up inputs
	public abstract void input(String message, ClickerClient client);

	// sends output to all the clients
	public abstract void output(String message);

	// sends output just to specified client
	public abstract void output(String message, ClickerClient client);

	public abstract void run() throws IOException, InterruptedException;

	int getNumClients();
}