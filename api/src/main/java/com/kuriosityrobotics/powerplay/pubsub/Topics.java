package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.util.Instant;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Topics {
	private Topics() {}

	public static Stream<? extends RobotTopic<?>> getMatchingTopicEntries(
			Map<String, Topic<?>> topics,
			Predicate<String> topicNamePredicate,
			Predicate<RobotDetails> robotPredicate) {
		HashSet<Map.Entry<String, Topic<?>>> set;
		synchronized (topics) {
			set = new HashSet<>(topics.entrySet());
		}
		return set.stream()
				.filter(entry -> topicNamePredicate.test(entry.getKey()))
				.flatMap(
						entry ->
								entry.getValue().activeRobotDetailsPublishingToThisTopic().stream()
										.filter(robotPredicate)
										.map(
												details ->
														new RobotTopic<>(
																entry.getKey(),
																entry.getValue(),
																details)))
				.filter(n -> !n.topic.messageType().isAnnotationPresent(Hidden.class))
				.sorted();
	}

	public static class RobotTopic<T> implements Comparable<RobotTopic<?>> {
		private final String topicName;
		private final Topic<T> topic;
		private final RobotDetails details;

		public RobotTopic(String topicName, Topic<T> topic, RobotDetails details) {
			this.topicName = topicName;
			this.topic = topic;
			this.details = details;
		}

		public Topic<?> topic() {
			return topic;
		}

		public RobotDetails details() {
			return details;
		}

		public String topicName() {
			return topicName;
		}

		public T lastValue() {
			return topic.lastValues().get(details);
		}

		public Instant lastValueTime() {
			return topic.lastValueTimes().get(details);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof RobotTopic
					&& topicName.equals(((RobotTopic<?>) obj).topicName)
					&& details.equals(((RobotTopic<?>) obj).details);
		}

		@Override
		public int hashCode() {
			return topicName.hashCode() ^ details.hashCode();
		}

		@Override
		public int compareTo(Topics.RobotTopic<?> o) {
			// First compare robot
			int result = details.compareTo(o.details);
			if (result != 0) return result;

			// Then compare topic name
			return topicName.compareTo(o.topicName);
		}
	}
}
