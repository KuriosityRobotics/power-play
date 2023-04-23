package com.kuriosityrobotics.powerplay.client;

import com.intellij.util.Consumer;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import com.kuriosityrobotics.powerplay.util.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Orchestrators {
	public static final Orchestrator CLIENT = Orchestrator.create("Client (IntelliJ)");
	public static final Set<RobotDetails> CONNECTED = ConcurrentHashMap.newKeySet();
	private static final Set<Consumer<RobotDetails>> ROBOT_ADDED_LISTENERS =
			ConcurrentHashMap.newKeySet();

	private static final Map<RobotDetails, Instant> disconnectionTimes =
			new ConcurrentHashMap<>(); // cooldown for reconnection

	private static final Set<Consumer<RobotDetails>> ROBOT_REMOVED_LISTENERS =
			ConcurrentHashMap.newKeySet();

	public static void addRobotAddedListener(Consumer<RobotDetails> listener) {
		ROBOT_ADDED_LISTENERS.add(listener);
	}

	public static void addRobotRemovedListener(Consumer<RobotDetails> listener) {
		ROBOT_REMOVED_LISTENERS.add(listener);
	}

	private static void onNewRobot(Object datum, String topicName, RobotDetails robot) {
		if (disconnectionTimes.containsKey(robot))
			if (Instant.now().isBefore(disconnectionTimes.get(robot).plusSeconds(2))) return;

		if (CONNECTED.add(robot))
			ROBOT_ADDED_LISTENERS.forEach(listener -> listener.consume(robot));
	}

	private static void onDisconnectedRobot(Object datum, String topicName, RobotDetails robot) {
		disconnectionTimes.put(robot, Instant.now());

		if (CONNECTED.remove(robot)) {
			ROBOT_REMOVED_LISTENERS.forEach(listener -> listener.consume(robot));
		}
	}

	static {
		CLIENT.subscribeToPattern(Pattern.compile(".*"), Orchestrators::onNewRobot);
		CLIENT.subscribe("goodbye", String.class, Orchestrators::onDisconnectedRobot);
		onNewRobot(null, null, CLIENT.robotDetails());
	}
}
