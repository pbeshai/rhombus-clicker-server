package ca.ubc.clicker.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ClientInputThread extends Thread {
	private int id;
	
	private BufferedReader in;
	private ClickerClient client;
	
	public ClientInputThread(int id, InputStream inputStream, ClickerClient client) {
		super("Client Input Thread " + id);
		this.client = client;
		in = new BufferedReader(new InputStreamReader(inputStream));

		start();
	}
	
	@Override
	public void run() {
		try {
			String inputLine;
			
			while ((inputLine = in.readLine()) != null) {
				client.input(inputLine);
			}
			
			in.close();
		} catch (IOException e) {
			System.out.println("IOException while running client "+id);
			e.printStackTrace();
		}
	}
}
