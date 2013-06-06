package ca.ubc.clicker.server.messages;

public class CommandResponseMessage {
	public String type = "command";
	public String command;
	public Object data;
}
