package com.kuriosityrobotics.powerplay.pubsub.bridge.message;

import com.kuriosityrobotics.powerplay.debug.StdoutTopicLogger;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RobotDetails implements Serializable, Comparable<RobotDetails> {
	private final String robotName;
	private final List<String> installedNodes;
	private final Set<NodeInfo> runningNodes;
	private final long nonce;

	public RobotDetails(String robotName, long robotId) {
		this.runningNodes = Collections.synchronizedSet(new ListOrderedSet<>());
		this.nonce = robotId;
		this.robotName = robotName;
		this.installedNodes =
				new Reflections(
								new ConfigurationBuilder()
										.forPackage("com.kuriosityrobotics")
										.addClassLoaders(
												StdoutTopicLogger.class
														.getClassLoader())) // add api classloader
						.getSubTypesOf(Node.class).stream()
								.filter(n -> !n.isAnnotationPresent(Hidden.class))
								.map(Class::getName)
								.collect(Collectors.toList());
	}

	public List<String> getInstalledNodes() {
		return installedNodes;
	}

	public Set<NodeInfo> runningNodes() {
		return runningNodes;
	}

	@Override
	public String toString() {
		return robotName + "@" + (nonce & 0xffffL);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RobotDetails) {
			return nonce == ((RobotDetails) obj).nonce;
		}
		return false;
	}

	public long nonce() {
		return nonce;
	}

	@Override
	public int hashCode() {
		return (int) nonce;
	}

	/**
	 * Returns a hash code that will state persistent across app restarts (not to be used for
	 * networking)
	 *
	 * @return the hashcode of the robot's name
	 */
	public int nameBasedHashCode() {
		return robotName.hashCode();
	}

	/**
	 * Uses the random nonces of two {@link RobotDetails} to decide whether this robot should be the
	 * client
	 *
	 * @param other the other robot details
	 * @return true if this robot should be the server
	 */
	public boolean shouldBeClient(RobotDetails other) {
		return nonce < other.nonce;
	}

	@Override
	public int compareTo(RobotDetails o) {
		return Long.compare(nonce, o.nonce);
	}
}
