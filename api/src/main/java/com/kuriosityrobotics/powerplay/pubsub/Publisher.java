package com.kuriosityrobotics.powerplay.pubsub;

/**
 * A <code>Publisher</code> is an object that publishes messages to a given topic.
 *
 * @param <T> The type of message published to the topic
 */
public final class Publisher<T> {
	private final String topic;
	private final Orchestrator orchestrator;

	Publisher(String topic, Orchestrator orchestrator) {
		this.topic = topic;
		this.orchestrator = orchestrator;
	}

	/**
	 * Publish a message to the topic.
	 *
	 * @param message The message to publish.
	 */
	public void publish(T message) {
		if (message == null) {
			throw new IllegalArgumentException("Message cannot be null");
		}

		orchestrator.dispatch(topic, message);
	}

	/**
	 * Makes the topic that this publisher publishes to replay missed messages to new subscribers.
	 */
	Publisher<T> replay() {
		orchestrator.getTopic(topic).get().setReplay(true);
		return this;
	}
}
