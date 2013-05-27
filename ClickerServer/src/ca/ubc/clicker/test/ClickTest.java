package ca.ubc.clicker.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * connects to a clicker server and sends many clicks
 * @author pbeshai
 *
 */
public class ClickTest {
	private static final String SERVER_HOST = "localhost";
	private static final int SERVER_PORT = 4444;
	private static final Random random = new Random(1);
	private static final int NUM_USERS = 5;
	
	private static final String[] ids = { // these ids will be used first before switching to TEST# id.
		"Peter",
		"Beshai",
		"INSTRUCTOR",
		"18981F9F",
		"1FC5AE74"
	};
	
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		// connect to clicker server
		Socket socket = new Socket(SERVER_HOST, SERVER_PORT);

		// send a stream of clicks
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		
		// init users
		List<ClickTestUser> users = new ArrayList<ClickTestUser>(NUM_USERS);
		for (int i = 0; i < NUM_USERS; i++) {
			String id = i < ids.length ? ids[i] : "TEST"+i;
			ClickTestUser user = new ClickTestUser(id, out, random);
			users.add(user);
		}
		
		while (true) {
			for (ClickTestUser user : users) {
				user.vote();
				if (random.nextInt(NUM_USERS / 2) == 0) Thread.sleep(random.nextInt(100) + 100); // occasionally put a small delay in 
			}
			Thread.sleep(random.nextInt(500) + 500);
		}
		
	}
}