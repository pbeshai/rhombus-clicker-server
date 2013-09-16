package ca.ubc.clicker.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import ca.ubc.clicker.server.gson.GsonFactory;
import ca.ubc.clicker.server.io.IOServer;
import ca.ubc.clicker.server.messages.ChoiceMessage;
import ca.ubc.clicker.server.messages.CommandMessage;

import com.google.gson.Gson;

public class SystemInputThread extends Thread {
	private IOServer server;
	private BufferedReader in;

	private static final String VOTE_COMMAND = "choose";

	public SystemInputThread(IOServer server) {
		super("SystemInputThread");
		this.server = server;
		in = new BufferedReader(new InputStreamReader(System.in));
		start();
	}

	private void voteCommand(String inputLine, CommandMessage message) {
		// specially interpret click to allow entry via: "click ID:Button ID:Button
		message.command = VOTE_COMMAND;

		if (inputLine.length() > VOTE_COMMAND.length() + 1) {
			// interpret arguments
			String args = inputLine.substring(VOTE_COMMAND.length() + 1);

			String[] clicks = args.split(" ");
			long time = new Date().getTime();
			ChoiceMessage[] choices = new ChoiceMessage[clicks.length];

			for (int i = 0; i < clicks.length; i++) {
				String[] split = clicks[i].split(":");
				if (split.length < 2) continue;
				choices[i] = new ChoiceMessage();
				choices[i].id = split[0];
				choices[i].choice = split[1];
				choices[i].time = time;
			}
			message.arguments = choices;
		}
	}
	

	@Override
	public void run() {
		try {
			String inputLine;
			Gson gson = GsonFactory.gson();
			while ((inputLine = in.readLine()) != null) {
				// convert into JSON
				CommandMessage message = new CommandMessage();
			
				if (inputLine.indexOf(VOTE_COMMAND) == 0) {
					voteCommand(inputLine, message);
				} else {
					message.command = inputLine;
				}

				server.input(gson.toJson(message));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
