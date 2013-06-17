package ca.ubc.clicker.test;

import java.io.PrintWriter;
import java.util.Random;

public class ClickTestUser {
	private static final String VOTE_STRING = "{'command':'choose','arguments':[{'id':'%s','choice':'%s'}]}";
	String id;
	PrintWriter out;
	Random random;
	String[] buttons = { "A", "B", "C", "D", "E" };

	ClickTestUser(String id, PrintWriter out, Random random) {
		this.id = id;
		this.out = out;
		this.random = random;
	}
	
	ClickTestUser(String id, PrintWriter out, Random random, String[] buttons) {
		this(id, out, random);
		this.buttons = buttons;
	}

	void vote(String button) {
		String vote = String.format(VOTE_STRING, id, button);
		out.println(vote);
		System.out.println(vote);
	}

	void vote() {
		vote(buttons[random.nextInt(buttons.length)]);
	}
}