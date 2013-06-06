package ca.ubc.clicker.server.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Reads input from the clicker server and outputs it to the IOServer
 * @author pbeshai
 *
 */
public class ClickerServerInputThread extends Thread {
	private BufferedReader in;
	private IOServer server;
	
	public ClickerServerInputThread(InputStream inputStream, IOServer server) {
		super("Clicker Server Input Thread");
		in = new BufferedReader(new InputStreamReader(inputStream));
		this.server = server;
		start();
	}
	
	@Override
	public void run() {
		try {
			String inputLine;
			
			while ((inputLine = in.readLine()) != null) {
				server.output(inputLine);
			}
			
			in.close();
		} catch (IOException e) {
			System.out.println("IOException while running clicker server input thread");
			e.printStackTrace();
		}
	}
}
