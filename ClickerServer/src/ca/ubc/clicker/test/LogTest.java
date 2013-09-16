package ca.ubc.clicker.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogTest {
	private static Logger clicksLog = LogManager.getLogger("clicks");
    public static void main(String[] args) {
    	clicksLog.info("Peter:A");
    }
}
