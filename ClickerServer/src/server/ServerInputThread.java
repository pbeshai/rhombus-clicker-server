package server;

import java.util.concurrent.BlockingQueue;

/**
 * Blocks on the input queue and runs the commands that are added
 * @author pbeshai
 *
 */
public class ServerInputThread extends Thread {
	private ClickerServer server;
	
	private final BlockingQueue<String> queue;
	
	public ServerInputThread(ClickerServer server, BlockingQueue<String> queue) {
		super("Server Input Listener Thread");
		
		this.server = server;
		this.queue = queue;
		
		start();
	}
	
	@Override
	public void run() {
		try {
			while (true) {
				String message = queue.take();
				server.runInput(message);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
