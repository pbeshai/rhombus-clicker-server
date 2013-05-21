package server;

import iClickerDriverOld.BaseClickerApp;
import iClickerDriverOld.ButtonEnum;
import iClickerDriverOld.ClickerException;
import iClickerDriverOld.FrequencyEnum;
import iClickerDriverOld.Vote;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import client.ClickerClient;

import com.codeminders.hidapi.HIDDeviceNotFoundException;
import com.codeminders.hidapi.HIDManager;

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
public class ClickerServer extends BaseClickerApp {
	
	public static final int DEFAULT_PORT = 4444;
	public static final String INSTRUCTOR_OUTPUT_ID = "INSTRUCTOR";
	
	private final BlockingQueue<String> inputQueue;
	private List<ClickerClient> clients;
	private int serverPort = DEFAULT_PORT;
	private CommandController commandController;
	private boolean baseStationConnected;
	private HIDManager hidManager;
	
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
		inputQueue = new LinkedBlockingQueue<String>();
		commandController = new CommandController(this);
		initOS();
		
		hidManager = new HIDManagerBaseStation(this);
		
		try {
			initDriver(hidManager);
			baseStationConnected = true;
		} catch (HIDDeviceNotFoundException e) {
			System.err.println("iClicker Base Station not connected.");
			baseStationConnected = false;
		}
		
		run();
	}
	
	public void run() throws IOException, InterruptedException {
		// start the device 
		if (baseStationConnected) {
			startBaseStation();
		}
		
		// start the input listener
		@SuppressWarnings("unused")
		ServerInputThread inputListenerThread = new ServerInputThread(this, inputQueue); 
		
		// start thread for reading stdin input
		@SuppressWarnings("unused")
		SystemInputThread systemInputThread = new SystemInputThread(this);
		
		
		// start thread for reading votes from the base station
		@SuppressWarnings("unused")
		ClickerThread clickerThread = new ClickerThread(this);
		
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		
		// bind the server to the right port
		if (serverPort >= 0) {
			try {
				serverSocket = new ServerSocket(serverPort);
				System.out.println("Successfully listening on port " + serverPort);
				
				// accept client connections
				try {
					while (true) {
						clientSocket = serverSocket.accept();
						ClickerClient client = new ClickerClient(clientSocket, this);
						clients.add(client);
					}
				} catch (IOException e) {
					System.err.println("Server socket accept failed on port: " + serverPort);
					System.exit(-1);
				}
				
				serverSocket.close();
				
			} catch (IOException e) {
				System.err.println("ERROR: Could not listen on port: " + serverPort);
			}
		}
		
		System.out.println("Failed to open socket on port " + serverPort + "; running locally");
		
		while (true) {
			Thread.sleep(300);
		}
		
	}
	
	public boolean isBaseStationConnected() {
		return baseStationConnected;
	}
	
	private void startBaseStation() {
		try {
			driver.startBaseStation();
			System.out.println("[device-start]");
		} catch (Exception e1) {
			System.err.println("ERROR: Failed to start base station.");
		}
	}
	
	public void baseStationAdded() {
		System.out.println("Base station added.");
		
		try {
			initDriver(hidManager);
			startBaseStation();
			acceptingVotes = false;
			baseStationConnected = true;
		} catch(IOException e) {
			System.err.println("Error initializing base station driver");
		} catch(InterruptedException e) {
			System.err.println("Interrupted while trying to initialize base station");
		}
	}
	
	public void baseStationRemoved() {
		System.out.println("Base station removed.");
		baseStationConnected = false;
		acceptingVotes = false;
		driver = null;
	}
	
	// public interface for others to queue up inputs
	public void input(String message) {
		try {
			inputQueue.add(message);
		} catch (IllegalStateException e) {
			System.err.println("Warning: no space left in server input queue");
		}
	}
	
	// interpret input (e.g. "vote start" to start accepting votes), to be used by the input listener
	void runInput(String message) {
		commandController.runCommand(message);
	}
	
	// sends output to all the clients
	public void output(String message) {
		System.out.println(message); // output locally for debugging
				
		// send to all the clients
		for (int i = 0; i < clients.size(); i++) {
			ClickerClient client = clients.get(i);
			// prune dead clients
			if(!client.isAlive()) {
				clients.remove(client);
				i--;
				continue;
			} 
			
			// broadcast message to each client
			client.output(message);
		}
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
		if (instructorId.equals(id)) {
			id = INSTRUCTOR_OUTPUT_ID;
		} 
		
		return String.format("%s:%s", id, button);
	}
	
	public List<Vote> votesFromString(String votesStr) {
		String[] votes = votesStr.trim().split(" ");
		
		List<Vote> voteList = new ArrayList<Vote>(votes.length);
		for (String vote : votes) {
			voteList.add(voteFromString(vote));
		}
		
		return voteList;
	}
	
	// vote of form ID:BUTTON, discard those with ID=INSTRUCTOR
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
			for (Vote vote : votes) {
				// instructor detected
				if(instructorId.equals(vote.getId())) {
					output(voteString(vote));
				} else {
					builder.append(voteString(vote));
					builder.append(" ");
				}
			}
			if(builder.length() > 0 && acceptingVotes) { // only output if we are accepting votes
				output(builder.toString().trim());
			}
		}
	}
	
	/**
	 * Usage: java ClickerServer [instructor-id [channel1 channel2 [port]]]
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String instructorId = "171BA6AA";
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
		
		@SuppressWarnings("unused")
		ClickerServer server = new ClickerServer(instructorId, channel1, channel2, port);
	}
}