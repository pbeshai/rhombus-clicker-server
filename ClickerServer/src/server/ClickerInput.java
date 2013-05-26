package server;

import client.ClickerClient;

public class ClickerInput {
	String message;
	ClickerClient client;
	
	public ClickerInput(String message) {
		this(message, null);
	}
	
	public ClickerInput(String message, ClickerClient client) {
		
		this.message = message;
		this.client = client;
	}
}
