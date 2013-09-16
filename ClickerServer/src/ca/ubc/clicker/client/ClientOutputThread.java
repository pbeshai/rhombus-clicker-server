package ca.ubc.clicker.client;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientOutputThread extends Thread {
	private PrintWriter out;
	private final BlockingQueue<String> queue;
	
	public ClientOutputThread(int id, OutputStream outStream) {
		super("ClientOutputThread" + id);
		out = new PrintWriter(outStream, true);
		queue = new LinkedBlockingQueue<String>();
		start();
	}
	
	@Override
	public void run() {
		// read in from the message queue and output across the socket
		try {
			while (true) {
				out.println(queue.take());
				
				// checkError true if we failed to write to output stream 
				// indicating socket closed.
				if(out.checkError()) {
					break;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		out.close();
	}
	
	public BlockingQueue<String> getMessageQueue() {
		return queue;
	}
}
