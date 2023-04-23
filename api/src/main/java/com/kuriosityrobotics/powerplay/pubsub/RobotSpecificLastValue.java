package com.kuriosityrobotics.powerplay.pubsub;

class RobotSpecificLastValue<T> extends LastValue<T> {
	private final Topics.RobotTopic<T> topic;

	RobotSpecificLastValue(Topics.RobotTopic<T> topic) {
		this.topic = topic;
	}

	@Override
	public T getValue() {
		var result = topic.lastValue();
		if (result == null) return defaultValue;
		return result;
	}
}
