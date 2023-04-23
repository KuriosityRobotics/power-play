package com.kuriosityrobotics.powerplay.pubsub;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.util.Instant;

import org.apache.commons.collections4.map.ListOrderedMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * A <code>Topic</code> is a pub/sub data channel. It keeps track of a list of subscribers, a list
 * of publishers and its message type.
 *
 * @param <T>
 */
public final class Topic<T> {
	private final LogInterface logInterface;

	private final RobotDetails local;
	private Class<T> messageType;
	private final Set<Subscription<? super T>> subscriptions;
	private final Set<Publisher<T>> publishers;

	final Map<Field, Node> lastValueHandles = new HashMap<>();
	private final ListOrderedMap<RobotDetails, T> lastValue = new ListOrderedMap<>();
	private final ListOrderedMap<RobotDetails, Instant> lastValueTime = new ListOrderedMap<>();

	private boolean replay = false;
	private final List<Object[]> replayBuffer = new Vector<>();

	public void setReplay(boolean replay) {
		this.replay = replay;
	}

	public boolean isReplay() {
		return replay;
	}

	Topic(
		LogInterface logInterface, RobotDetails local,
		Class<T> messageType,
		Set<Subscription<? super T>> subscriptions,
		Set<Publisher<T>> publishers) {
		this.logInterface = logInterface;
		this.local = local;
		this.messageType = messageType;
		this.subscriptions = subscriptions;
		this.publishers = publishers;
	}

	public Topic(LogInterface logInterface, RobotDetails local, Class<T> messageType) {
		this(logInterface, local, messageType, ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
	}

	public Map<RobotDetails, T> lastValues() {
		return Collections.synchronizedMap(lastValue);
	}

	public Set<RobotDetails> activeRobotDetailsPublishingToThisTopic() {
		return lastValue.keySet();
	}

	public synchronized T lastValue() {
		return lastValue.get(local);
	}

	public synchronized Instant lastValueTime() {
		return lastValueTime.get(local);
	}

	public synchronized ListOrderedMap<RobotDetails, Instant> lastValueTimes() {
		return lastValueTime;
	}

	public synchronized void setLastValue(RobotDetails robotDetails, T lastValue) {
		this.lastValue.put(robotDetails, lastValue);
		this.lastValueTime.put(robotDetails, Instant.now());
	}

	void removeSubscription(Subscription<? super T> subscription) {
		subscriptions.remove(subscription);
	}

	public void addLastValueHandle(Node target, Field handle) {
		handle.setAccessible(true);
		lastValueHandles.put(handle, target);
	}

	public Class<T> messageType() {
		return messageType;
	}

	void setMessageType(Class<T> messageType) {
		this.messageType = messageType;
	}

	public ListenableFuture<?> handleMessage
		(
			LogInterface logInterface,
			Executor executor,
			T message,
			String topicName,
			RobotDetails robot
		) {
		if (replay) {
			replayBuffer.add(new Object[]{message, topicName, robot});
		}

		for (var handle : lastValueHandles.entrySet()) {
			try {
				handle.getKey().set(handle.getValue(), message);
			} catch (IllegalAccessException e) {
				logInterface.err(e);
			}
		}

		synchronized (this) {
			notifyAll();
		}

		var futures = new ArrayList<ListenableFuture<?>>();
//		var state = StackUnwinder.saveCurrent();
		subscriptions.forEach(
			subscription -> {
				var future =
					Futures.submit(
						() -> {
//							StackUnwinder.publishStackElementThreadLocal.set(state);
							subscription.handle(message, topicName, robot);
						}, executor);
				futures.add(
					Futures.catching(
						future,
						Throwable.class,
						e -> {
							logInterface.err(e);
							return null;
						},
						Runnable::run));
			});

		return Futures.allAsList(futures);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Topic<?>) obj;
		return Objects.equals(this.messageType, that.messageType)
			&& Objects.equals(this.subscriptions, that.subscriptions)
			&& Objects.equals(this.publishers, that.publishers);
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageType, subscriptions, publishers);
	}

	@Override
	public String toString() {
		return "Topic["
			+ "messageType="
			+ messageType
			+ ", "
			+ "subscriptions="
			+ subscriptions
			+ ", "
			+ "publishers="
			+ publishers
			+ ']';
	}

	public void addSubscription(Subscription<? super T> sub) {
		subscriptions.add(sub);
		if (replay) {
			replayBuffer.forEach(
				arr -> {
					var message = (T) arr[0];
					var topicName = (String) arr[1];
					var robot = (RobotDetails) arr[2];
					sub.handle(message, topicName, robot);
				});
		}
	}

	public void addPublisher(Publisher<T> pub) {
		publishers.add(pub);
	}

	public T waitForMessage() throws InterruptedException {
		synchronized (this) {
			T message;
			do {
				this.wait();
				message = lastValue.get(local);
			} while (message == null);

			return message;
		}
	}
}
