package ca.ubc.clicker.server;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ca.ubc.clicker.client.ClickerClient;
import ca.ubc.clicker.server.io.BaseIOServer;
import ca.ubc.clicker.server.io.IOServer;
import ca.ubc.clickers.BaseClickerApp;
import ca.ubc.clickers.Vote;
import ca.ubc.clickers.driver.exception.ClickerException;
import ca.ubc.clickers.enums.ButtonEnum;
import ca.ubc.clickers.enums.FrequencyEnum;

/**
 * The runnable application. A server that continuously requests votes from the 
 * clicker base station and broadcasts them to listeners (clients). Also takes input on
 * standard in and over the socket from the clients.
 * 
 * Usage: java ClickerServer [instructor-id [channel1 channel2 [port]]]
 * 
 * @author pbeshai
 *
 */
public class ClickerServer extends BaseClickerApp implements IOServer {
	public static final int DEFAULT_PORT = 4444;
	public static final String INSTRUCTOR_OUTPUT_ID = "INSTRUCTOR";
	
	private final BlockingQueue<ClickerInput> inputQueue;
	private List<ClickerClient> clients;
	private int serverPort = DEFAULT_PORT;
	private CommandController commandController;
	private IOServer io;
	
	public ClickerServer() throws InterruptedException, IOException, ClickerException {
		this(null);
	}
	
	public ClickerServer(String instructorId) throws InterruptedException, IOException, ClickerException {
		this(instructorId, null, null);
	}
	
	public ClickerServer(String instructorId, FrequencyEnum channel1, FrequencyEnum channel2) throws InterruptedException, IOException, ClickerException {
		this(instructorId, null, null, DEFAULT_PORT);
	}
	
	public ClickerServer(String instructorId, FrequencyEnum channel1, FrequencyEnum channel2, Integer port)
			throws InterruptedException, IOException, ClickerException {
		super(instructorId, channel1, channel2);
		
		if (port != null) this.serverPort = port;
		LCDRow1 = "Clicker Server";
		clients = new LinkedList<ClickerClient>();
		inputQueue = new LinkedBlockingQueue<ClickerInput>();
		commandController = new CommandController(this);
		io = new BaseIOServer(serverPort, this);
	}
	
	public void init() throws IOException {
		// start the input listener
		@SuppressWarnings("unused")
		ServerInputThread inputListenerThread = new ServerInputThread(this, inputQueue); 
		
		// start thread for reading stdin input
		@SuppressWarnings("unused")
		SystemInputThread systemInputThread = new SystemInputThread(this);
		
		
		// start thread for reading votes from the base station
		@SuppressWarnings("unused")
		ClickerThread clickerThread = new ClickerThread(this);
	}
	
	public void run() throws IOException, InterruptedException {
		init();
		io.run();
	}
	
	// queue up input without specifying clicker client
	@Override
	public void input(String message) {
		input(message);
	}
	
	// public interface for others to queue up inputs
	@Override
	public void input(String message, ClickerClient client) {
		try {
			inputQueue.add(new ClickerInput(message, client));
		} catch (IllegalStateException e) {
			System.err.println("Warning: no space left in server input queue");
		}
	}
	
	// interpret input (e.g. "vote start" to start accepting votes), to be used by the input listener
	void runInput(ClickerInput input) {
		commandController.runCommand(input);
	}
	
	// sends output to all the clients
	@Override
	public void output(String message) {
		output(message, null, true);
	}
	
	// sends output just to specified client
	@Override
	public void output(String message, ClickerClient client) {
		output(message, client, true);
	}
	
	
	// sends output to all the clients if client == null, otherwise just to client
	public void output(String message, ClickerClient client, boolean printLocal) {
		if (printLocal) {
			System.out.println(message); // output locally for debugging
		}
		
		io.output(message, client);
	}
	
	public int getNumClients() {
		return clients.size();
	}
	
	// formats a vote for output over the socket (ID:BUTTON)
	// if id == instructor, output is INSTRUCTOR:BUTTON
	public String voteString(Vote vote) {
		return voteString(vote.getId(), vote.getButton().name());
	}
	
	// formats a vote for output over the socket (ID:BUTTON)
	// if id == instructor, output is INSTRUCTOR:BUTTON
	public String voteString(String id, String button) { 
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		if (instructorId.equals(id)) {
			builder.append("\"instructor\":true,");
		}
		builder.append("\"id\":\"");
		builder.append(id);
		builder.append("\",\"choice\":\"");
		builder.append(button);
		builder.append("\"}");
		
		return builder.toString();
	}
	
	public List<Vote> votesFromString(String votesStr) {
		String[] votes = votesStr.trim().split(" ");
		
		List<Vote> voteList = new ArrayList<Vote>(votes.length);
		for (String vote : votes) {
			voteList.add(voteFromString(vote));
		}
		
		return voteList;
	}
	
	// vote of form ID:BUTTON
	public Vote voteFromString(String vote) {
		// break into id and button
		String[] parts = vote.split(":");
		String id = parts[0].trim();
		String button = parts[1].trim();
		
		// ignore the id generated from the constructor and use the one from the string.
		Vote v = new Vote("111111", ButtonEnum.valueOf(button));
		v.setId(id);
		
		return v;
	}
	
	public void outputVotes(List<Vote> votes) {
		// assemble a vote string
		if (!votes.isEmpty()) {
			StringBuilder builder = new StringBuilder();			
			int numVotes = 0;
			for (Vote vote : votes) {
				// instructor detected
				if(instructorId.equals(vote.getId())) {
					output("["+voteString(vote)+"]");
				} else {
					if (numVotes == 0) {
						builder.append("[");
					} else {
						builder.append(",");
					}
					builder.append(voteString(vote));
					numVotes += 1;
				}
			}
			if(numVotes > 0 && acceptingVotes) { // only output if we are accepting votes
				builder.append("]");
				output(builder.toString().trim());
			}
		}
	}
	
	/**
	 * Usage: java ClickerServer [instructor-id [channel1 channel2 [port]]]
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String instructorId = "371BA68A"; //"171BA6AA";
		FrequencyEnum channel1 = null, channel2 = null;
		Integer port = null;
		if (args.length > 0) {
			instructorId = args[0];
		}
		if (args.length > 2) {
			channel1 = FrequencyEnum.valueOf(args[1]);
			channel2 = FrequencyEnum.valueOf(args[2]);
		}
		if (args.length > 3) {
			port = Integer.parseInt(args[3]);
		}
	
		System.out.println("Instructor ID: " + instructorId);
		System.out.println("Channel1: " + (channel1 == null ? DEFAULT_CHANNEL_1 : channel1));
		System.out.println("Channel2: " + (channel2 == null ? DEFAULT_CHANNEL_2 : channel2));
		System.out.println("Port: " + (port == null ? DEFAULT_PORT : port));
		
		ClickerServer server = new ClickerServer(instructorId, channel1, channel2, port);
		server.run();
	}
}