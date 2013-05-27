package ca.ubc.clicker.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SystemInputThread extends Thread {
	private ClickerServer server;
	private BufferedReader in;
	
	public SystemInputThread(ClickerServer server) {
		super("Input Thread");
		this.server = server;
		in = new BufferedReader(new InputStreamReader(System.in));
		start();
	}
	
	@Override
	public void run() {
		try {
			String inputLine;
			
			while ((inputLine = in.readLine()) != null) {
				server.input(inputLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
