package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import java.util.function.Consumer;

/**
 * A subscription is an object that 'listens' for messages on a given topic. When a message is
 * published to its topic, the subscription's callback is called with the message on a thread pool
 * managed by the Orchestrator.
 *
 * @param <T>
 */
public class Subscription<T> {
	private final MessageConsumer<? super T, String, RobotDetails> callback;

	Subscription(MessageConsumer<? super T, String, RobotDetails> callback) {
		this.callback = callback;
	}

	Subscription(Consumer<? super T> callback) {
		this.callback = (datum, topicName_, robotDetails_) -> callback.accept(datum);
	}

	/**
	 * Handles a message using the callback
	 *
	 * @param message the message to handle
	 */
	void handle(T message, String topicName, RobotDetails originatingRobot) {
	   try {
		  callback.accept(message, topicName, originatingRobot);
	   } catch (Throwable e) {
		  var st = StackUnwinder.unwind();
		  var n = new StackTraceElement[st.length + 1];
		  System.arraycopy(st, 0, n, 1, st.length);
		  n[0] = e.getStackTrace()[0]; // TODO: this is a hack, work out why the unwinder doesnt include the first element
		  e.setStackTrace(n);
		  throw e;
	   }
	}
}
