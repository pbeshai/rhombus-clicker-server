package ca.ubc.clicker.server;

import java.util.LinkedList;
import java.util.List;

import ca.ubc.clicker.client.ClickerClient;
import ca.ubc.clicker.server.messages.CommandResponseMessage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * Class responsible for interpreting and running commands on the server
 * @author pbeshai
 *
 */
public class CommandController {
	public static final String COMMAND_PING = "ping"; // to show we are connected
	public static final String COMMAND_START_VOTING = "start voting";
	public static final String COMMAND_STOP_VOTING = "stop voting";
	public static final String COMMAND_STATUS = "status";
	public static final String COMMAND_VOTE = "vote";
	
	private final ClickerServer server;
	private final JsonParser parser;
	private final List<Command> commands;
	
	public CommandController(ClickerServer server) {
		this.server = server;
		this.parser = new JsonParser();
		this.commands = new LinkedList<Command>();
		registerCommands();
	}
	
	private Gson gson() {
		return server.gson();
	}

	private void outputCommandResponse(String command, Object data, ClickerClient client, boolean printLocal) {
		CommandResponseMessage message = new CommandResponseMessage();
		message.command = command;
		message.data = data;
		server.output(gson().toJson(message), client, printLocal);
	}
	
	// Oh, how I dream of anonymous functions. Java 8 has them!
	private void registerCommands() {
		// does not broadcast
		commands.add(new Command(COMMAND_PING) { void run(JsonElement args, ClickerClient client) throws Exception {
			outputCommandResponse(COMMAND_PING, null, client, false);
		} });
		
		// start voting
		commands.add(new Command(COMMAND_START_VOTING) { void run(JsonElement args, ClickerClient client) throws Exception {
			server.startAcceptingVotes();
			outputCommandResponse(COMMAND_START_VOTING, null, null, true);
		} });
		
		// stop voting
		commands.add(new Command(COMMAND_STOP_VOTING) { void run(JsonElement args, ClickerClient client) throws Exception {
			server.stopAcceptingVotes();
			outputCommandResponse(COMMAND_STOP_VOTING, null, null, true);
		} });
		
		// get status - instructor id, accepting votes, number of clients
		// does not broadcast
		commands.add(new Command(COMMAND_STATUS) { void run(JsonElement args, ClickerClient client) throws Exception {
			outputCommandResponse(COMMAND_STATUS, server.getStatus(), client, true);
		} });
		
		// click received (as opposed to via clicker base station)
		commands.add(new Command(COMMAND_VOTE) { void run(JsonElement votesJson, ClickerClient client) throws Exception {
			server.outputVotes(server.votesFromJson(votesJson));
		} });
	}
	
	
	public void runCommand(ClickerInput input) {
		String message = input.message; // is a CommandMessage object (JSON)
		ClickerClient client = input.client;
		String name = null;
		JsonElement args = null;

		// deserialize as a generic json obj so we can interpret the arguments later.
		try {
			JsonObject jsonObj = parser.parse(message).getAsJsonObject();
			name = jsonObj.get("command").getAsString();
			args = jsonObj.get("arguments");
		} catch (IllegalStateException e) {
			System.err.println("Error running command: " + e.getMessage());
			return;
		}
		
				
		try {
			for (Command command : commands) {
				if (command.name.equals(name)) {
					command.run(args, client);
					return;
				}
			}

			System.err.println("Warning! Unable to find command for "+message);
		} catch (Exception e) {
			server.outputError(message);
			System.out.println("Exception while running command "+message);
			e.printStackTrace();
		}
	}
	
	// class to be instantiated anonymously since we can't do anonymous functions
	private abstract class Command {
		String name; // what the user types to activate
		
		Command(String input) {
			this.name = input;
		}
		
		abstract void run(JsonElement args, ClickerClient client) throws Exception;
	}
}
