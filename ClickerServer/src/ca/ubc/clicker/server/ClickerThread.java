package ca.ubc.clicker.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.ubc.clicker.Vote;
import ca.ubc.clicker.driver.exception.ClickerException;

/**
 * Continuously reads votes from the base station
 * @author pbeshai
 *
 */
public class ClickerThread extends Thread {
	private static Logger log = LogManager.getLogger();
	private final static int SLEEP_TIME = 150;
	
	ClickerServer server;
	String instructorId;
	
	
	ClickerThread(ClickerServer server) {
		super("ClickerThread");
		this.server = server;
		this.instructorId = server.getInstructorId();
		start();
	}
	
	@Override
	public void run() {
		List<Vote> votes = new ArrayList<Vote>();
		while (true) {
			try {
					
				if (server.isBaseStationConnected()) {
					votes = server.getDriver().requestVotes();
				
					server.outputChoices(votes);
				}
				Thread.sleep(SLEEP_TIME);			
			} catch (InterruptedException e) {
				log.error("Interrupted: "+e.getMessage());
				e.printStackTrace();
				break;
			} catch (IOException e) {
				log.error("IOException: "+e.getMessage());
				e.printStackTrace();
			} catch (ClickerException e) {
				log.error("ClickerException: "+e.getMessage());
				e.printStackTrace();
			}
		}
		log.error("Exiting...");
	}
}
