package server;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Class responsible for interpreting and running commands on the server
 * @author pbeshai
 *
 */
public class CommandController {
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
	private void registerCommands() {
		
		// start voting
		commands.add(new Command(COMMAND_START_VOTING) { void run(String args) throws Exception {
			server.startAcceptingVotes();
			server.output("[vote start]\n");
		} });
		
		// stop voting
		commands.add(new Command(COMMAND_STOP_VOTING) { void run(String args) throws Exception {
			server.stopAcceptingVotes();
			server.output("[vote stop]\n");
		} });
		
		// get status - instructor id, accepting votes, number of clients
		commands.add(new Command(COMMAND_STATUS) { void run(String args) throws Exception {
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
			
			server.output(message.toString());
		} });
		
		// click received (as opposed to via clicker base station)
		commands.add(new Command(COMMAND_CLICK) { void run(String votesStr) throws Exception {
			server.outputVotes(server.votesFromString(votesStr));
		} });
	}
	
	
	public void runCommand(String input) {
		// split into command and args
		String name, args;
		int separator = input.indexOf(COMMAND_SEPARATOR);
		if (separator != -1) {
			name = input.substring(0, separator);
			args = input.substring(separator + 1);
		} else {
			name = input;
			args = "";
		}
		
		try {
			for (Command command : commands) {
				if (command.name.equals(name)) {
					command.run(args);
					return;
				}
			}

			System.err.println("Warning! Unable to find command for "+input);
		} catch (Exception e) {
			server.output("[error:"+input+"]\n");
			System.out.println("Exception while running command "+input);
			e.printStackTrace();
		}
	}
	
	// class to be instantiated anonymously since we can't do anonymous functions
	private abstract class Command {
		String name; // what the user types to activate
		
		Command(String input) {
			this.name = input;
		}
		
		abstract void run(String args) throws Exception;
	}
}
