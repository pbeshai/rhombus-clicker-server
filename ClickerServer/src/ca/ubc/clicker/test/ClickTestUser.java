package ca.ubc.clicker.test;

import java.io.PrintWriter;
import java.util.Random;

public class ClickTestUser {
	private static final String VOTE_STRING = "click|%s:%s";
	String id;
	PrintWriter out;
	Random random;

	ClickTestUser(String id, PrintWriter out, Random random) {
		this.id = id;
		this.out = out;
		this.random = random;
	}

	void vote(String button) {
		String vote = String.format(VOTE_STRING, id, button);
		out.println(vote);
		System.out.println(vote);
	}

	void vote() {
		vote(String.valueOf((char)(random.nextInt(5)+'A')));
	}
}