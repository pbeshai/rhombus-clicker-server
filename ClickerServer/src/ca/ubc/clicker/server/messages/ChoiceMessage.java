package ca.ubc.clicker.server.messages;

public class ChoiceMessage {
	public String id;
	public String choice; // aka vote or button pressed
	public Boolean instructor;
	public Long time;
}
