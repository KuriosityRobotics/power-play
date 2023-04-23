package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.debug.StdoutTopicLogger;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.NodeInfo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.util.ExceptionRunnable;

import com.kuriosityrobotics.powerplay.util.Instant;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public interface Orchestrator extends AutoCloseable, LogInterface {

	static String defaultToString(Object o) {
		return o.getClass().getSimpleName() + "@" + Integer.toHexString(o.hashCode());
	}

	Random random = new Random();

	static Orchestrator create(String robotName) {
		return new OrchestratorImpl(
			new RobotDetails(robotName, robotName.hashCode()), true, true, null);
	}

	static Orchestrator createTest(RobotDetails robotDetails, boolean startNetwork) {
		var impl = new OrchestratorImpl(robotDetails, true, startNetwork, null);
		impl.setBlockingDispatch(true);
		impl.startNode("debug", new StdoutTopicLogger(impl));
		return impl;
	}

	static Orchestrator createTest(String robotName, boolean startNetwork) {
		return createTest(new RobotDetails(robotName, random.nextLong()), startNetwork);
	}

	void startBridge();

	void setBlockingDispatch(boolean blockingDispatch);

	/**
	 * Blocks until a message is received on the given topic.
	 *
	 * @param topic The topic to listen on.
	 * @return The message received.
	 */
	<T> T waitForMessage(String topic) throws InterruptedException;

	/**
	 * Optionally returns a Topic with the given name and message type
	 *
	 * @param messageType the type of message of the desired topic
	 * @param topicName   the topic name
	 * @param <T>         the type of the topic
	 * @return a Topic over T, if this topic exists
	 * @throws IllegalArgumentException if the message type does not match the topic's registered
	 *                                  type
	 */
	<T> Optional<Topic<T>> getTopic(String topicName, Class<T> messageType);

	/**
	 * Optionally returns a Topic with the given name
	 *
	 * @param topicName the topic name
	 * @return the topic
	 */
	Optional<Topic<?>> getTopic(String topicName);

	/**
	 * Adds a topic
	 *
	 * @param topicName   the topic name
	 * @param messageType the message type to be sent down the topic
	 * @param <T>         the topic type
	 * @return the newly added topic
	 * @throws IllegalArgumentException if the topic already exists
	 */
	<T> Topic<T> addTopic(String topicName, Class<T> messageType);

	<T> Topic<T> getOrAddTopic(String topicName, Class<T> messageType);

	/**
	 * Sends a message to a topic
	 *
	 * @param topicName the topic this should be sent to
	 * @param message   the message
	 * @param <T>       the message type
	 */
	<T> void dispatch(String topicName, RobotDetails originatingRobot, T message);

	<T> void dispatch(String topicName, T message);

	<T> void dispatchSynchronous(String topicName, RobotDetails originatingRobot, T message);

	<T> void dispatchSynchronous(String topicName, T message);

	/**
	 * Subscribes to all the specified topics, calling the callback when a message is published to
	 * any of them
	 *
	 * @param callback the callback to call when a message is published to any of the topics
	 * @param topics   the topics to subscribe to
	 */
	void listenForAny(Runnable callback, String... topics);

	/**
	 * Subscribes to a topic
	 *
	 * @param topicName   the name of the topic
	 * @param messageType the type of the topic
	 * @param callback    to be called when a message is sent to the topic
	 * @param <T>         the type param for the topic type
	 * @return a Subscriber whose callback will be called whenever a message is sent to the topic
	 */
	<T> Subscription<T> subscribe(String topicName, Class<T> messageType, Consumer<T> callback);

	/**
	 * Subscribes to a topic
	 *
	 * @param topicName   the name of the topic
	 * @param messageType the type of the topic
	 * @param callback    to be called when a message is sent to the topic
	 * @param <T>         the type param for the topic type
	 * @return a Subscriber whose callback will be called whenever a message is sent to the topic
	 */
	<T> Subscription<T> subscribe(
		String topicName,
		Class<T> messageType,
		MessageConsumer<T, String, RobotDetails> callback);

	/**
	 * Subscribes to all current (and future-added) topics whose names are matched by the provided
	 * {@link Pattern}.
	 *
	 * @param namePattern the pattern to match topic names against
	 * @param callback    the function to call on messages sent to topics matching the requested {@link
	 *                    Pattern}
	 */
	Subscription<Object> subscribeToPattern(
		Pattern namePattern, MessageConsumer<Object, String, RobotDetails> callback);

	/**
	 * Subscribes to all current (and future-added) topics whose names are matched by the provided
	 * {@link Pattern}.
	 *
	 * @param namePattern the pattern to match topic names against
	 * @param callback    the function to call on messages sent to topics matching the requested {@link
	 *                    Pattern}
	 */
	Subscription<Object> subscribeToPattern(
		Pattern namePattern, BiConsumer<Object, String> callback);

	/**
	 * Creates a {@link Publisher} for a topic
	 *
	 * @param topicName   the name of the topic
	 * @param messageType the type of the topic
	 * @param <T>         the type param for the topic type
	 * @return a {@link Publisher} for the topic
	 */
	<T> Publisher<T> publisher(String topicName, Class<T> messageType);

	/**
	 * Creates a {@link LastValue} whose referent is the specified topic and robot
	 *
	 * @param topicName   the name of the topic
	 * @param messageType the type of the topic
	 * @param robot       the robot to use as the originator of the last value
	 * @param <T>         the type param for the topic type
	 * @return a LastValue which can be used to get the last message sent to the topic
	 * @see LastValue
	 */
	<T> LastValue<T> lastValue(String topicName, Class<T> messageType, RobotDetails robot);

	/**
	 * Creates a {@link LastValue} whose referent is the specified topic
	 *
	 * @param topicName   the name of the topic
	 * @param messageType the type of the topic
	 * @param <T>         the type param for the topic type
	 * @return a LastValue which can be used to get the last message sent to the topic
	 * @see LastValue
	 */
	<T> LastValue<T> lastValue(String topicName, Class<T> messageType);

	/**
	 * Starts a {@link Node}.
	 *
	 * <p>This will assign the given {@link Node} the given name, and start periodically calling the
	 * Node's update function.
	 *
	 * @param name the name of the node
	 * @param node the node to be started
	 */
	void startNode(String name, Node node);

	void fireMessageAt(Object datum, String topicName, RobotDetails target);

	ScheduledExecutorService nodeExecutorService();

	void registerNode(NodeInfo requestedNode, Node node);

	/**
	 * Stops a {@link Node}
	 *
	 * @param name the name of the node
	 */
	void stopNode(String name);

	/**
	 * Stops a {@link Node}
	 *
	 * @param node the node to be stopped
	 */
	void stopNode(Node node);

	/**
	 * @return an immutable {@link Map} of all registered topics and their names
	 */
	Map<String, Topic<?>> getTopics();

	/**
	 * Removes a {@link Subscription} from all topics.
	 *
	 * <p>This is useful when a {@link Subscription} is no longer needed. It will be removed from
	 * all topics it was subscribed to.
	 *
	 * @param subscription the subscription to remove
	 */
	void removeSubscription(Subscription<?> subscription);

	/**
	 * Gets a {@link Set} of all connected {@link RobotDetails}. The returned set is unmodifiable.
	 *
	 * @return an unmodifiable {@link Set} of all connected {@link RobotDetails}
	 */
	Set<RobotDetails> connectedRobots();

	void markNodeBeingConstructed(Node node);

	RobotDetails robotDetails();

	/**
	 * Runs an {@link ExceptionRunnable} after a certain time in milliseconds.
	 *
	 * @param millis   time in milliseconds to wait before executing runnable.
	 * @param runnable ExceptionRunnable to run after millis.
	 * @return a {@link ScheduledFuture} that can be used to cancel the scheduled runnable.
	 */
	ScheduledFuture<?> setTimer(long millis, ExceptionRunnable runnable);

	/**
	 * Returns the start time of the robot.
	 *
	 * @return the start time of the robot
	 */
	Instant startTime();

	CompletableFuture<Void> startActionAsync(String actionName);
	CompletableFuture<Void> startActionAsync(Node.NodeTimer action);


	void close();

	void actionStarted();

	void actionFinished();

	void awaitActionCompletion() throws InterruptedException;
	boolean actionsDone();

	void addAction(Node node, String actionName, Node.NodeTimer action);
}
