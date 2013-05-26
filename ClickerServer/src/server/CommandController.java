package server;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import client.ClickerClient;


/**
 * Class responsible for interpreting and running commands on the server
 * @author pbeshai
 *
 */
public class CommandController {
	public static final String COMMAND_PING = "ping"; // to show we are connected
	public static final String COMMAND_START_VOTING = "vote start";
	public static final String COMMAND_STOP_VOTING = "vote stop";
	public static final String COMMAND_STATUS = "status";
	public static final String COMMAND_CLICK = "click";
	private static final char COMMAND_SEPARATOR = '|'; // separators command from args. e.g. click|id:button 
	
	private final ClickerServer server;
	
	private final List<Command> commands;
	
	public CommandController(ClickerServer server) {
		this.server = server;
		this.commands = new LinkedList<Command>();
		registerCommands();
	}
	
	// Oh, how I dream of anonymous functions. Java 7 has them!
	// TODO: probably differentiate between commands that send reply to 1 client vs. broadcasting to all
	private void registerCommands() {
		// does not broadcast
		commands.add(new Command(COMMAND_PING) { void run(String args, ClickerClient client) throws Exception {
			server.output("[ping]\n", client, false);
		} });
		
		// start voting
		commands.add(new Command(COMMAND_START_VOTING) { void run(String args, ClickerClient client) throws Exception {
			server.startAcceptingVotes();
			server.output("[vote start]\n");
		} });
		
		// stop voting
		commands.add(new Command(COMMAND_STOP_VOTING) { void run(String args, ClickerClient client) throws Exception {
			server.stopAcceptingVotes();
			server.output("[vote stop]\n");
		} });
		
		// get status - instructor id, accepting votes, number of clients
		// does not broadcast
		commands.add(new Command(COMMAND_STATUS) { void run(String args, ClickerClient client) throws Exception {
			StringBuilder message = new StringBuilder();
			
			message.append("[status]\n");
			
			message.append("Time: ");
			message.append(new Date().getTime());
			message.append("\n");
			
			message.append("Instructor ID: ");
			message.append(server.getInstructorId());
			message.append("\n");
			
			message.append("Accepting Votes: ");
			message.append(server.isAcceptingVotes());
			message.append("\n");
			
			message.append("# Clients: ");
			message.append(server.getNumClients());
			message.append("\n");
			
			server.output(message.toString(), client);
		} });
		
		// click received (as opposed to via clicker base station)
		commands.add(new Command(COMMAND_CLICK) { void run(String votesStr, ClickerClient client) throws Exception {
			// TODO: this shouldn't crash with bad format
			server.outputVotes(server.votesFromString(votesStr));
		} });
	}
	
	
	public void runCommand(ClickerInput input) {
		String message = input.message;
		ClickerClient client = input.client;
		
		// split into command and args
		String name, args;
		int separator = message.indexOf(COMMAND_SEPARATOR);
		if (separator != -1) {
			name = message.substring(0, separator);
			args = message.substring(separator + 1);
		} else {
			name = message;
			args = "";
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
			server.output("[error:"+message+"]\n");
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
		
		abstract void run(String args, ClickerClient client) throws Exception;
	}
}
