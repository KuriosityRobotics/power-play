package com.kuriosityrobotics.powerplay.pubsub;

import java.util.Map;

class AnyRobotLastValue<T> extends LastValue<T> {
	private final Topic<T> topic;

	AnyRobotLastValue(Topic<T> topic) {
		this.topic = topic;
	}

	@Override
	public T getValue() {
		return mostRecentlyPublished();
	}

	private T mostRecentlyPublished() {
		return topic.lastValueTimes().entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.map(topic.lastValues()::get)
				.orElse(defaultValue);
	}
}
