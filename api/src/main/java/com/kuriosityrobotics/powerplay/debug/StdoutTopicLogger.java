package com.kuriosityrobotics.powerplay.debug;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

public class StdoutTopicLogger extends Node {
	private static final boolean DEBUG = true, INFO = true, WARN = true, ERROR = true;

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	public StdoutTopicLogger(Orchestrator orchestrator) {
		super(orchestrator);
	}

	@SubscribedTo(topic = "log/.*", isPattern = true)
	public void handle(String message, String topicName, RobotDetails originatingRobot) {
		if ("log/debug".equals(topicName)) {
			if (DEBUG) System.out.printf("%10s:  %s%n", "DEBUG", message);
		} else if ("log/info".equals(topicName)) {
			if (INFO) System.out.printf(ANSI_GREEN + "%10s:  %s%n" + ANSI_RESET, "INFO", message);
		} else if ("log/warn".equals(topicName)) {
			if (WARN) System.err.printf(ANSI_YELLOW + "%10s:  %s%n" + ANSI_RESET, "WARN", message);
		} else if ("log/err".equals(topicName)) {
			if (ERROR) System.err.printf(ANSI_RED + "%10s:  %s%n" + ANSI_RESET, "ERROR", message);
		} else {
			System.out.printf("%10s:  %s%n", topicName, message);
		}
	}
}
