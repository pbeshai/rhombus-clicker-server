package ca.ubc.clicker.server;

import java.util.concurrent.BlockingQueue;

/**
 * Blocks on the input queue and runs the commands that are added
 * @author pbeshai
 *
 */
public class ServerInputThread extends Thread {
	private ClickerServer server;
	
	private final BlockingQueue<ClickerInput> queue;
	
	public ServerInputThread(ClickerServer server, BlockingQueue<ClickerInput> queue) {
		super("Server Input Listener Thread");
		
		this.server = server;
		this.queue = queue;
		
		start();
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				ClickerInput input = queue.take();
				server.runInput(input);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
