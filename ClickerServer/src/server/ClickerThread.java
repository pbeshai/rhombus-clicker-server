package server;

import java.io.IOException;
import java.util.ArrayList;

import iClickerDriverOld.ClickerException;
import iClickerDriverOld.IClickerDriverOld;
import iClickerDriverOld.Vote;

/**
 * Continuously reads votes from the base station
 * @author pbeshai
 *
 */
public class ClickerThread extends Thread {
	private final static int SLEEP_TIME = 150;
	
	ClickerServer server;
	String instructorId;
	
	
	ClickerThread(ClickerServer server) {
		super("Clicker Thread");
		this.server = server;
		this.instructorId = server.getInstructorId();
		start();
	}
	
	@Override
	public void run() {
		ArrayList<Vote> votes = new ArrayList<Vote>();
		while (true) {
			try {
					
				if (server.isBaseStationConnected()) {
					votes = server.getDriver().requestVotes();
				
					server.outputVotes(votes);
				}
				Thread.sleep(SLEEP_TIME);			
			} catch (InterruptedException e) {
				System.err.println("clicker thread interrupted: "+e.getMessage());
				e.printStackTrace();
				break;
			} catch (IOException e) {
				System.err.println("clicker thread IOException: "+e.getMessage());
				e.printStackTrace();
			} catch (ClickerException e) {
				System.err.println("clicker thread ClickerException: "+e.getMessage());
				e.printStackTrace();
			}
		}
		System.err.println("exiting clicker thread");
	}
}
