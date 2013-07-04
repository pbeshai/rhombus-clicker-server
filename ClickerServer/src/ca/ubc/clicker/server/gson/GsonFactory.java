package ca.ubc.clicker.server.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * shared Gson configuration here 
 * @author pbeshai
 *
 */
public class GsonFactory {
	private static GsonBuilder builder = new GsonBuilder();
	
	public static Gson gson() {
		return builder.create();
	}
}
