package ca.ubc.clicker.tmp;

import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import ca.ubc.clicker.server.filters.Filter;

public class FilterLoader {
	
	public static void main(String[] args) {
		List<Filter> filters = new LinkedList<Filter>();
		ServiceLoader<Filter> filterLoader = ServiceLoader.load(Filter.class);
		for (Filter f : filterLoader) {
			filters.add(f);
			System.out.println(f);
		}
	}
}