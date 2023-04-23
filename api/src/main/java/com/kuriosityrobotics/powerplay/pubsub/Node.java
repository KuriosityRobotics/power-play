package com.kuriosityrobotics.powerplay.pubsub;

import static com.kuriosityrobotics.powerplay.util.StringUtils.toDisplayString;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.kuriosityrobotics.powerplay.util.Duration;
import com.kuriosityrobotics.powerplay.util.ExceptionProducer;
import com.kuriosityrobotics.powerplay.util.ExceptionRunnable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A <code>Node</code> is a core component of the robot. It has an optional update function, and
 * contains modularised functionality for a subsystem - for example, <code>Odometry</code> or <code>
 * MotionModelKalmanFilter</code>.
 *
 * <p>As soon as a {@link Node} calls {@link Node#Node} at the beginning of its
 * constructor, the orchestrator becomes aware of the Node and registers it with a temporary name of
 * the format <code>[IN CONSTRUCTION] ClassName@unique_id</code>.
 */
public abstract class Node implements AutoCloseable {
	final Set<Subscription<?>> boundSubscriptions = new HashSet<>();
	private final Set<ScheduledFuture<?>> periodicTasks = new HashSet<>();

	/**
	 * The name of the node. This is used for logging and debugging. Field is injected by {@link
	 * Orchestrator} and is what is passed to Orchestrator when this node is started.
	 */
	protected final Orchestrator orchestrator;

	/**
	 * Adds a task to the node's periodic task list, to be used by {@link #close()}.
	 */
	final void startPeriodicTask(Runnable task, int frequency) {
		var scheduledTask =
			orchestrator
				.nodeExecutorService()
				.scheduleWithFixedDelay(task, 0, (long) (1000. / frequency), MILLISECONDS);
		periodicTasks.add(scheduledTask);
	}

	/**
	 * Registers the node with the orchestrator using {@link
	 * Orchestrator#markNodeBeingConstructed(Node)}. This will NOT start calling the update
	 * function, and will give this node instance a placeholder name
	 *
	 * <p>This exists so telemetry works inside the constructor
	 *
	 * @param orchestrator the orchestrator on which to start the node
	 */
	protected Node(Orchestrator orchestrator) {
		this.orchestrator = orchestrator;
		orchestrator.markNodeBeingConstructed(this);
	}

	/**
	 * Logs an error message, and throws if in debug mode. This is used for debugging.
	 *
	 * @param message the message to log
	 */
	protected void err(String message) {
		orchestrator.err(message, this);
	}

	/**
	 * Logs the given exception, throwing if in debug mode
	 *
	 * @param e the exception to log
	 */
	protected void err(Throwable e) {
		orchestrator.err(e, this);
	}

	/**
	 * Logs a warning message. This is used for debugging.
	 *
	 * @param message the message to log
	 */
	protected void warn(String message) {
		orchestrator.warn(message, this);
	}

	/**
	 * Logs the given exception as a warning. This is used for debugging.
	 *
	 * @param e the exception to log
	 */
	protected void warn(Throwable e) {
		orchestrator.warn(e, this);
	}

	/**
	 * Logs an info message. This is used for debugging.
	 *
	 * @param message the message to log
	 */
	protected void info(String message) {
		orchestrator.info(message, this);
	}

	/**
	 * Logs a debug message. This is used for debugging.
	 *
	 * @param message the message to log
	 */
	protected void debug(String message) {
		orchestrator.debug(message, this);
	}

	/**
	 * Asserts that the given condition is true. This is used for debugging.
	 *
	 * @param condition the condition to assert
	 * @param message   the message to log if the condition is false
	 * @return the condition
	 */
	protected boolean assertThat(boolean condition, String message) {
		return orchestrator.assertThat(condition, message);
	}

	/**
	 * Asserts that the given condition is true. This is used for debugging.
	 *
	 * @param condition the condition to assert
	 * @param format    the message to log if the condition is false (the message is formatted using
	 *                  {@link String#format(String, Object...)})
	 * @param args      the arguments to format the message with
	 */
	protected boolean assertThat(boolean condition, String format, Object... args) {
		return orchestrator.assertThat(condition, String.format(format, args));
	}

	/**
	 * Asserts that the given condition is true. This is used for debugging.
	 *
	 * @param condition the condition to assert
	 */
	protected boolean assertThat(boolean condition) {
		return orchestrator.assertThat(condition);
	}

	/**
	 * Wraps the given supplier in a try-catch, and logs the exception and returns null if it
	 * occurs.
	 *
	 * @param runnable
	 * @param <T>
	 * @return
	 */
	protected <T> T wrapException(ExceptionProducer<T> runnable) {
		try {
			return runnable.produce();
		} catch (Throwable e) {
			e.setStackTrace(StackUnwinder.unwind());
			err(e);
			return null;
		}
	}

	/**
	 * Tries to wrap the given supplied in a try-catch, and logs the exception if it occurs.
	 */
	protected <T> Optional<T> tryWrapException(ExceptionProducer<T> producer) {
		try {
			return Optional.ofNullable(producer.produce());
		} catch (Throwable e) {
			warn(e);
			return Optional.empty();
		}
	}

	/**
	 * Sends a value with the given caption as telemetry
	 */
	protected void telemetry(String caption, Object value) {
		orchestrator.telemetry(caption, toDisplayString(value));
	}

	/**
	 * Wraps the given {@link Runnable} in a try-catch, and logs the exception if it occurs.
	 *
	 * @param runnable the runnable to wrap
	 */
	protected void wrapException(ExceptionRunnable runnable) {
		try {
			runnable.run();
		} catch (Throwable e) {
			err(e);
		}
	}

	/**
	 * To make sure update never gets called after it's been stopped (fixes some weird race
	 * condition in {@link java.util.concurrent.ScheduledFuture#cancel}
	 */
	boolean stopped = false;

	@Override
	public void close() {
		orchestrator.info("Stopping " + this);
		HashSet<Runnable> toDelete = new HashSet<>();
		orchestrator
			.getTopics()
			.forEach(
				(k, v) -> v.lastValueHandles.forEach(
					(handle, node) -> {
						if (node == this) {
							toDelete.add(() -> v.lastValueHandles.remove(handle));
						}
					}));

		toDelete.forEach(Runnable::run);
		boundSubscriptions.forEach(orchestrator::removeSubscription);
		periodicTasks.forEach(n -> n.cancel(true));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	private final ReentrantLock lock = new ReentrantLock();

	public interface NodeTimer {
		void start() throws Throwable;
	}
}
