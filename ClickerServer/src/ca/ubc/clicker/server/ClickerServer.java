package ca.ubc.clicker.server;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.ubc.clicker.BaseClickerApp;
import ca.ubc.clicker.Vote;
import ca.ubc.clicker.client.ClickerClient;
import ca.ubc.clicker.driver.exception.ClickerException;
import ca.ubc.clicker.enums.ButtonEnum;
import ca.ubc.clicker.enums.FrequencyEnum;
import ca.ubc.clicker.server.filters.Filter;
import ca.ubc.clicker.server.gson.GsonFactory;
import ca.ubc.clicker.server.io.BaseIOServer;
import ca.ubc.clicker.server.io.IOServer;
import ca.ubc.clicker.server.messages.ChoiceMessage;
import ca.ubc.clicker.server.messages.ErrorMessage;
import ca.ubc.clicker.server.messages.ResponseMessage;
import ca.ubc.clicker.server.messages.StatusMessage;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

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
	private static Logger log = LogManager.getLogger();
	private static Logger clicksLog = LogManager.getLogger("clicks");
	
	public static final int DEFAULT_PORT = 4444;
	
	private static final String CONFIG_PROPERTIES_FILE = "config.properties";
	
	private final BlockingQueue<ClickerInput> inputQueue;
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
		super(instructorId, channel1, channel2, "Clicker Server", String.format("I:%s Ch:%s%s", instructorId, channel1.name(), channel2.name()));
		
		if (port != null) this.serverPort = port;
		
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
		SystemInputThread systemInputThread = new SystemInputThread(io);
		
		
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
		input(message, null);
	}
	
	// public interface for others to queue up inputs
	@Override
	public void input(String message, ClickerClient client) {
		try {
			inputQueue.add(new ClickerInput(message, client));
		} catch (IllegalStateException e) {
			log.error("Warning: no space left in server input queue");
		}
	}
	
	// interpret input (e.g. "enable choices" to start accepting votes), to be used by the input listener
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
			if (client != null) {
				log.info("[output for {}] {}", client.toString(), message);
			} else {
				log.info("[output] {}", message);
			}
		}
		
		io.output(message, client);
	}
	
	public int getNumClients() {
		return io.getNumClients();
	}
	
	private ChoiceMessage choiceMessage(Vote vote) {
		return choiceMessage(vote.getId(), vote.getButton().name());
	}
	
	private ChoiceMessage choiceMessage(String id, String button) {
		ChoiceMessage message = new ChoiceMessage();
		message.id = id;
		message.choice = button;
		if (instructorId.equals(id)) {
			message.instructor = true;
		}
		message.time = new Date().getTime();
		
		return message;
	}
	
	private Vote voteFromChoiceMessage(ChoiceMessage message) {
		// ignore the id generated from the constructor and use the one from the message
		if (message == null || message.choice == null) {
			return null;
		}
		
		String voteButton = message.choice.toUpperCase();
		try { 
			Vote vote = new Vote("111111", ButtonEnum.valueOf(voteButton));
			vote.setId(message.id);
			return vote;
		} catch (IllegalArgumentException e) {
			log.error("Discarding illegal vote "+voteButton+" by "+message.id);
		}
		return null;
	}
	
	// json is serialized collection of VoteMessage objects
	public List<Vote> votesFromJson(JsonElement choicesJson) {
		ChoiceMessage[] messages = gson().fromJson(choicesJson, ChoiceMessage[].class);
		
		if (messages == null) {
			return null;
		}
		
		List<Vote> votes = new ArrayList<Vote>(messages.length);
		
		for (ChoiceMessage message : messages) {
			Vote vote = voteFromChoiceMessage(message);
			if (vote != null) {
				votes.add(vote);
			}
		}
		
		return votes;
	}
	
	// use List<Vote> since it's easy to get from the base station. 
	// only outputs instructor votes if accepting votes is false
	public void outputChoices(List<Vote> votes) {
		if (votes == null || votes.isEmpty()) {
			return;
		}
		
		
		// convert to voteMessage list
		List<ChoiceMessage> messages = new ArrayList<ChoiceMessage>(votes.size());
		for (Vote vote : votes) {
			// only output instructor votes if accepting votes is false
			if (isAcceptingVotes() || instructorId.equals(vote.getId())) {
				clicksLog.info("{}:{}", vote.getId(), vote.getButton());
				messages.add(choiceMessage(vote));
			}
		}
		
		if (messages.isEmpty()) { // abort if no votes 
			return;
		}
		
		ResponseMessage message = new ResponseMessage();
		message.type = "choices";
		message.data = messages;
		output(gson().toJson(message));
	}
	
	public void outputError(String errorStr, String command) {
		ErrorMessage message = new ErrorMessage();
		message.error = errorStr;
		message.command = command;
		output(gson().toJson(message));
	}
	
	public StatusMessage getStatus() {
		StatusMessage status = new StatusMessage();
		status.acceptingChoices = isAcceptingVotes();
		status.numClients = getNumClients();
		status.instructorId = getInstructorId();
		status.time = new Date().getTime();
		
		return status;
	}
	

	private Gson gson() {
		return GsonFactory.gson();
	}
	
	public void initializeFilter(Filter filter) {
		filter.initialize(this);
	}
	
	/**
	 * Usage: java ClickerServer [instructor-id [channel1 channel2 [port]]]
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String instructorId = "371BA68A"; //"171BA6AA";
		FrequencyEnum channel1 = DEFAULT_CHANNEL_1, channel2 = DEFAULT_CHANNEL_2;
		Integer port = DEFAULT_PORT;
		
		// read from config.properties file
		Properties config = new Properties();
		try {
			config.load(new FileInputStream(CONFIG_PROPERTIES_FILE));
			port = Integer.valueOf(config.getProperty("port", String.valueOf(DEFAULT_PORT)));
			instructorId = config.getProperty("instructorId", instructorId);
			channel1 = FrequencyEnum.valueOf(config.getProperty("channel1", DEFAULT_CHANNEL_1.name()));
			channel2 = FrequencyEnum.valueOf(config.getProperty("channel2", DEFAULT_CHANNEL_2.name()));
		} catch (IOException e) {
			log.error("Could not find config.properties");
		}
		
		// override from arguments
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
	
		log.info("Starting Clicker Server...");
		log.info("Instructor ID: " + instructorId);
		log.info("Channel1: " + channel1);
		log.info("Channel2: " + channel2);
		log.info("Port: " + port);
		
		ClickerServer server = new ClickerServer(instructorId, channel1, channel2, port);
		server.run();
	}
}