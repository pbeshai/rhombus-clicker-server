package ca.ubc.clicker.server.filters;

import ca.ubc.clicker.server.ClickerServer;

/**
 * Interface for filtering input/output on a server.
 * @author pbeshai
 *
 */
public interface Filter {
	public boolean initialize(ClickerServer server);
	
	public String output(String message);
	
	public String input(String message);
}
