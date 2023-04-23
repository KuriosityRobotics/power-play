package com.kuriosityrobotics.powerplay.pubsub;

import static com.google.common.primitives.Primitives.unwrap;
import static com.google.common.primitives.Primitives.wrap;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.google.common.collect.HashBiMap;
import com.google.common.io.ByteStreams;
import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.BidirectionalBridge;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.NodeInfo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.util.ExceptionRunnable;
import com.kuriosityrobotics.powerplay.util.Instant;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import nu.pattern.OpenCV;

/**
 * The Orchestrator manages subscriptions and publishers to topics and <code>Node</code> lifecycles.
 */
@SuppressWarnings({"unchecked"})
class OrchestratorImpl implements Orchestrator {
	protected final boolean debugMode;

	static {
		try {
			OpenCV.loadShared();
		} catch (Throwable e) {
//			System.load(
//				"/usr/local/opencv/share/java/opencv4/lib"
//					+ Core.NATIVE_LIBRARY_NAME
//					+ ".dylib");
		}
	}

	private final Instant startTime = Instant.now();

	private final Object hardwareMap;
	private final RobotDetails robotDetails;

	protected final ThreadPoolExecutor callbackExecutorService;
	protected final ScheduledExecutorService nodeExecutorService;
	protected final ActionExecutor actionExecutor;


	protected final PubsubAnnotationBinder pubsubAnnotationBinder;

	private final Map<String, Topic<?>> topics;

	private final Map<String, Node.NodeTimer> actions;
	private final Map<Node, Set<String>> nodeActions;

	private final HashBiMap<Pattern, Subscription<?>> topicPatternSubscriptions;
	private final HashBiMap<String, Node> nodes;

	protected BidirectionalBridge bridge;
	/**
	 * If this is true, dispatch() will block until the callback queue is empty. This is useful for
	 * testing
	 */
	private boolean blockingDispatch = false;

	/**
	 * Creates a new {@link Orchestrator}. This will create two thread pool: the first one is for
	 * executing callbacks of publishers. It has a default size of 0, and will grow up to 8 threads.
	 * The second thread pool has a fixed size, and runs the update functions for Nodes.
	 */
	public OrchestratorImpl(
		RobotDetails robotDetails,
		boolean debugMode,
		boolean startNetwork,
		Object hardwareMap) {
		this.robotDetails = robotDetails;
		this.debugMode = debugMode;

		this.pubsubAnnotationBinder = new PubsubAnnotationBinder(this);

		this.topics = Collections.synchronizedMap(new TreeMap<>());
		this.actions = Collections.synchronizedMap(new TreeMap<>());
		this.nodeActions = new ConcurrentHashMap<>();
		this.topicPatternSubscriptions = HashBiMap.create();
		this.nodes = HashBiMap.create();

		this.nodeExecutorService = Executors.newScheduledThreadPool(2, this::createThread);

		this.callbackExecutorService =
			new ThreadPoolExecutor(
				2,
				8,
				60L,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>() {
					@Override
					public boolean offer(@NotNull Runnable runnable) {
						try {
							put(runnable);
							return true;
						} catch (InterruptedException e) {
							return false;
						}
					}
				},
				this::createThread,
				(runnable, executor) -> runnable.run());
		// action executor service has infinite threads
		this.actionExecutor = new ActionExecutor(this::createThread);

		this.hardwareMap = hardwareMap;
		assertThat(
			hardwareMap == null
				|| hardwareMap.getClass().getSimpleName().equals("HardwareMap"));

		startNode("helper", new OrchestratorHelper());

		Runtime.getRuntime().addShutdownHook(createThread(this::close));

		if (startNetwork) startBridge();
	}

	protected Thread createThread(Runnable runnable) {
		var thread = new Thread(runnable);
		thread.setUncaughtExceptionHandler(this::onUncaughtException);
		return thread;
	}


	protected void onUncaughtException(Thread source, Throwable t) {
		t.printStackTrace();
		t.setStackTrace(StackUnwinder.unwind());
		t.printStackTrace();
		err(t);
		if (debugMode) close();
	}

	@Override
	public void startBridge() {
		if (bridge == null) {
			bridge = new BidirectionalBridge(this);
			startNode("bridge", bridge);
		}
	}

	@Override
	public void setBlockingDispatch(boolean blockingDispatch) {
		this.blockingDispatch = blockingDispatch;
	}

	@Override
	public <T> T waitForMessage(String topic) throws InterruptedException {
		var topic_ = getOrAddTopic(topic, Object.class);
		return (T) topic_.waitForMessage();
	}

	@Override
	public <T> Optional<Topic<T>> getTopic(String topicName, Class<T> messageType) {
		Topic<?> topic = topics.get(topicName);
		if (topic == null) return empty();

		if (topic.messageType() == Object.class && topic.messageType().isPrimitive()) {
			//noinspection rawtypes
			topic.setMessageType(
				(Class) wrap(messageType)); // terrible hack to get around type erasure
			warn("Topic " + topicName + " has been updated to " + topic.messageType());
		}

		return of((Topic<T>) topic);
	}

	@Override
	public Optional<Topic<?>> getTopic(String topicName) {
		Topic<?> topic = topics.get(topicName);
		if (topic == null) return empty();

		return of(topic);
	}

	@Override
	public <T> Topic<T> addTopic(String topicName, Class<T> messageType) {
		if (topics.get(topicName) != null)
			throw new IllegalArgumentException("Topic " + topicName + " already exists.");

		var topic = new Topic<>(this, robotDetails, unwrap(messageType));

		topicPatternSubscriptions.forEach(
			(topicPattern, subscription) -> {
				if (topicPattern.matcher(topicName).matches())
					topic.addSubscription((Subscription<? super T>) subscription);
			});

		topics.put(topicName, topic);

		return topic;
	}

	public Set<Node> getNodes() {
		return nodes.values();
	}

	@Override
	public synchronized <T> Topic<T> getOrAddTopic(String topicName, Class<T> messageType) {
		return this.getTopic(topicName, messageType)
			.orElseGet(() -> addTopic(topicName, messageType));
	}

	public <T> void dispatch(String topicName, RobotDetails originatingRobot, T message, boolean synchronous) {
		if (topicName == null) throw new IllegalArgumentException("Topic name cannot be null");
		if (originatingRobot == null)
			throw new IllegalArgumentException("Originating robot cannot be null");
		if (message == null) throw new IllegalArgumentException("Message cannot be null");

		var topic = this.getOrAddTopic(topicName, (Class<T>) message.getClass());
		topic.setLastValue(originatingRobot, message);

		if (synchronous) {
			topic.handleMessage(
				this, Runnable::run, message, topicName, originatingRobot
			);
			return;
		}

		var future =
			topic.handleMessage(
				this, callbackExecutorService, message, topicName, originatingRobot);
		if (blockingDispatch) {
			try {
				future.get(500, TimeUnit.MILLISECONDS);
			} catch (TimeoutException | InterruptedException | ExecutionException e) {
				System.err.println("NAME OF TIMEING OUT TOPIC:  " + topicName + ", MESSAGE:  " + message);
				err(e);
			}
		}
	}

	public <T> void dispatch(String topicName, T message, boolean synchronous) {
		dispatch(topicName, robotDetails, message);
	}

	@Override
	public <T> void dispatch(String topicName, RobotDetails originatingRobot, T message) {
		dispatch(topicName, originatingRobot, message, false);
	}

	@Override
	public <T> void dispatch(String topicName, T message) {
		dispatch(topicName, robotDetails, message, false);
	}

	@Override
	public <T> void dispatchSynchronous(String topicName, RobotDetails originatingRobot, T message) {
		dispatch(topicName, originatingRobot, message, true);
	}

	@Override
	public <T> void dispatchSynchronous(String topicName, T message) {
		dispatch(topicName, robotDetails, message, true);
	}

	@Override
	public void listenForAny(Runnable callback, String... topics) {
		for (var topic : topics) subscribe(topic, Object.class, _n -> callback.run());
	}

	@Override
	public <T> Subscription<T> subscribe(
		String topicName, Class<T> messageType, Consumer<T> callback) {
		var sub = new Subscription<>(callback);

		var topic = getOrAddTopic(topicName, messageType);
		topic.addSubscription(sub);
		return sub;
	}

	@Override
	public <T> Subscription<T> subscribe(
		String topicName,
		Class<T> messageType,
		MessageConsumer<T, String, RobotDetails> callback) {
		var sub = new Subscription<T>(callback);
		var topic = getOrAddTopic(topicName, messageType);
		topic.addSubscription(sub);
		return sub;
	}

	@Override
	public Subscription<Object> subscribeToPattern(
		Pattern namePattern, MessageConsumer<Object, String, RobotDetails> callback) {
		var subscription = new Subscription<>(callback);
		topicPatternSubscriptions.put(namePattern, subscription);
		topics.forEach(
			(topicName, topic) -> {
				if (namePattern.matcher(topicName).matches())
					topic.addSubscription(subscription);
			});

		return subscription;
	}

	@Override
	public Subscription<Object> subscribeToPattern(
		Pattern namePattern, BiConsumer<Object, String> callback) {
		return subscribeToPattern(
			namePattern, (datum, topicName, __) -> callback.accept(datum, topicName));
	}

	@Override
	public <T> Publisher<T> publisher(String topicName, Class<T> messageType) {
		var pub = new Publisher<T>(topicName, this);

		var topic = getOrAddTopic(topicName, messageType);
		topic.addPublisher(pub);
		return pub;
	}

	@Override
	public <T> LastValue<T> lastValue(String topicName, Class<T> messageType, RobotDetails robot) {
		return new RobotSpecificLastValue<>(
			new Topics.RobotTopic<>(topicName, getOrAddTopic(topicName, messageType), robot));
	}

	@Override
	public <T> LastValue<T> lastValue(String topicName, Class<T> messageType) {
		return new AnyRobotLastValue<>(getOrAddTopic(topicName, messageType));
	}

	@Override
	public void startNode(String name, Node node) {
		if (nodes.containsKey(name)) {
			warn("Not starting node with name " + name + " twice.");
			return;
		}
		registerNode(new NodeInfo(node.getClass().getName(), name), node);
	}

	@Override
	public void fireMessageAt(Object datum, String topicName, RobotDetails target) {
		if (target.equals(robotDetails)) {
			dispatch(topicName, datum);
			return;
		}
		bridge.distributeMessage(datum, topicName, target);
	}

	@Override
	public ScheduledExecutorService nodeExecutorService() {
		return nodeExecutorService;
	}

	@Hidden
	private class OrchestratorHelper extends Node {
		OrchestratorHelper() {
			super(OrchestratorImpl.this);
		}

		@SubscribedTo(topic = "node/requestStart")
		void handleStartRequest(NodeInfo requestedNode)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
			InstantiationException, IllegalAccessException {
			Node node = null;
			try {
				var constructor =
					(Constructor<Node>)
						getClass()
							.getClassLoader()
							.loadClass(requestedNode.nodeType())
							.getDeclaredConstructor();
				constructor.setAccessible(true);
				node = constructor.newInstance();
			} catch (NoSuchMethodException e) {
				try {
					var constructor =
						(Constructor<Node>)
							getClass()
								.getClassLoader()
								.loadClass(requestedNode.nodeType())
								.getDeclaredConstructor(Orchestrator.class);
					constructor.setAccessible(true);
					node = constructor.newInstance(OrchestratorImpl.this);
				} catch (NoSuchMethodException e1) {
					if (hardwareMap == null) {
						throw e1;
					}
					var constructor =
						(Constructor<Node>)
							getClass()
								.getClassLoader()
								.loadClass(requestedNode.nodeType())
								.getDeclaredConstructor(
									Orchestrator.class, hardwareMap.getClass());
					constructor.setAccessible(true);
					node = constructor.newInstance(OrchestratorImpl.this, hardwareMap);
				}
			} catch (Throwable e) {
				err(e);
			} finally {
				if (node == null)
					err(
						"Failed to create node "
							+ requestedNode.nodeType()
							+ ".  Constructor must have signature (Orchestrator) or, if"
							+ " running on a robot, (Orchestrator, HardwareMap)");
			}

			registerNode(requestedNode, node);
		}
	}

	@Override
	public void registerNode(NodeInfo requestedNode, Node node) {
		debug(
			"Starting updates for node "
				+ Orchestrator.defaultToString(node)
				+ " as "
				+ requestedNode.nodeName());
		pubsubAnnotationBinder.bindMethods(node);
		nodes.inverse().forcePut(node, requestedNode.nodeName());

		if (!node.getClass().isAnnotationPresent(Hidden.class)) {
			robotDetails.runningNodes().add(requestedNode);
			dispatch("node/started", requestedNode);
		}
	}

	public void markNodeBeingConstructed(Node node) {
		info("Constructing node " + Orchestrator.defaultToString(node));
		pubsubAnnotationBinder.bindFields(node);
		nodes.inverse().put(node, "[IN CONSTRUCTION] " + Orchestrator.defaultToString(node));
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void stopNode(String name) {
		var node = nodes.get(name);
		if (assertThat(node != null, "Attempted to stop node " + name + ", but it is not running."))
			stopNode(node);
	}

	@Override
	public void stopNode(Node node) {
		node.stopped = true;
		node.close();
		nodeActions.getOrDefault(node, Collections.emptySet()).forEach(this::unregisterAction);
		nodeActions.remove(node);
		var removedName = nodes.inverse().remove(node);
		robotDetails.runningNodes().removeIf(n -> n.nodeName().equals(removedName));
	}

	@Override
	public Map<String, Topic<?>> getTopics() {
		return this.topics;
	}

	@Override
	public void removeSubscription(Subscription<?> subscription) {
		var values = new HashMap<>(topics);
		for (var topic : values.values())
			topic.removeSubscription((Subscription<? super Object>) subscription);
	}

	@Override
	public Set<RobotDetails> connectedRobots() {
		if (bridge != null) return bridge.getConnectedRobots();
		else return Collections.emptySet();
	}

	private boolean closed = false;

	@Override
	public void close() {
		setBlockingDispatch(true);
		info("closing");
		nodes.forEach((k, v) -> stopNode(k));
		nodeExecutorService.shutdown();
		callbackExecutorService.shutdown();

		try {
			nodeExecutorService.awaitTermination(500, TimeUnit.MILLISECONDS);
			callbackExecutorService.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (Throwable e) {
			e.printStackTrace();
			nodeExecutorService.shutdownNow();
			callbackExecutorService.shutdownNow();
		}
		if (bridge != null) bridge.close();
		closed = true;
	}

	@Override
	@SuppressWarnings("deprecation")
	protected void finalize() {
		if (!closed) close();
	}

	@Override
	public RobotDetails robotDetails() {
		return robotDetails;
	}

	@Override
	public ScheduledFuture<?> setTimer(long millis, ExceptionRunnable runnable) {
		return nodeExecutorService.schedule(() -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				err(e);
			}
		}, millis, TimeUnit.MILLISECONDS);
	}

	private static PrintStream printlnStreamFromConsumer(Consumer<String> consumer) {
		return new PrintStream(ByteStreams.nullOutputStream()) {
			@Override
			public void println(String x) {
				consumer.accept(x);
			}
		};
	}

	@Override
	public void err(String msg) {
		getErrorStream().accept(msg);
	}

	@Override
	public void err(String msg, Node originatingNode) {
		var nodeName = getNodeName(originatingNode);
		assertThat(
			nodeName != null,
			"Attempted to send debug message from node "
				+ originatingNode
				+ ", but it is "
				+ "not"
				+ " running.");
		getErrorStream(originatingNode).accept(msg);
	}

	@Override
	public void err(Throwable e) {
		e.printStackTrace(printlnStreamFromConsumer(getErrorStream()));
	}

	@Override
	public void err(Throwable e, Node originatingNode) {
		var nodeName = getNodeName(originatingNode);
		assertThat(
			nodeName != null,
			"Attempted to send debug message from node "
				+ originatingNode
				+ ", but it is "
				+ "not"
				+ " running.");
		e.printStackTrace(printlnStreamFromConsumer(getErrorStream(originatingNode)));
	}

	@Override
	public void warn(String msg) {
		getWarnStream().accept(msg);
	}

	@Override
	public void warn(String msg, Node originatingNode) {
		var nodeName = getNodeName(originatingNode);
		assertThat(
			nodeName != null,
			"Attempted to send debug message from node "
				+ originatingNode
				+ ", but it is "
				+ "not"
				+ " running.");
		getWarnStream(originatingNode).accept(msg);
	}

	@Override
	public void warn(Throwable e, Node originatingNode) {
		var nodeName = getNodeName(originatingNode);
		assertThat(
			nodeName != null,
			"Attempted to send debug message from node "
				+ originatingNode
				+ ", but it is "
				+ "not"
				+ " running.");
		e.printStackTrace(printlnStreamFromConsumer(getWarnStream(originatingNode)));
	}

	@Override
	public void info(String msg) {
		getInfoStream().accept(msg);
	}

	@Override
	public void info(String msg, Node originatingNode) {
		var nodeName = getNodeName(originatingNode);
		assertThat(
			nodeName != null,
			"Attempted to send debug message from node "
				+ originatingNode
				+ ", but it is "
				+ "not"
				+ " running.");
		getInfoStream(originatingNode).accept(msg);
	}

	@Override
	public void debug(String msg) {
		getDebugStream().accept(msg);
	}

	@Override
	public void debug(String msg, Node originatingNode) {
		var nodeName = getNodeName(originatingNode);
		assertThat(
			nodeName != null,
			"Attempted to send debug message from node "
				+ originatingNode
				+ ", but it is "
				+ "not"
				+ " running.");
		getDebugStream(originatingNode).accept(msg);
	}

	protected String getNodeName(Node originatingNode) {
		return nodes.inverse().getOrDefault(originatingNode, originatingNode.getClass().getSimpleName());
	}

	protected Consumer<String> getErrorStream() {
		return x -> System.err.printf("(err): %s%n", x);
	}

	protected Consumer<String> getErrorStream(Node originatingNode) {
		return msg -> System.err.printf("[%s] (err): %s%n", getNodeName(originatingNode), msg);
	}

	protected Consumer<String> getWarnStream() {
		return x -> System.err.printf("(warn): %s%n", x);
	}

	protected Consumer<String> getWarnStream(Node originatingNode) {
		return msg -> System.err.printf("[%s] (warn): %s%n", getNodeName(originatingNode), msg);
	}

	protected Consumer<String> getInfoStream() {
		return x -> System.out.printf("(info): %s%n", x);
	}

	protected Consumer<String> getInfoStream(Node originatingNode) {
		return msg -> System.out.printf("[%s] (info): %s%n", getNodeName(originatingNode), msg);
	}

	protected Consumer<String> getDebugStream() {
		return x -> System.out.printf("(debug): %s%n", x);
	}

	protected Consumer<String> getDebugStream(Node originatingNode) {
		return msg -> System.out.printf("[%s] (debug): %s%n", getNodeName(originatingNode), msg);
	}

	@SuppressWarnings("unchecked")
	protected static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
		throw (T) t;
	}

	@Override
	public void telemetry(String name, Object obj) {
		dispatch("telemetry/" + name, obj.toString());
	}

	@Override
	public void telemetry(String name, Object obj, Node originatingNode) {
		var nodeName = getNodeName(originatingNode);
		assertThat(
			nodeName != null,
			"Attempted to send debug message from node "
				+ originatingNode
				+ ", but it is "
				+ "not"
				+ " running.");
		telemetry(name, format("(%s) %s", nodeName, obj.toString()));
	}

	@Override
	public boolean assertThat(boolean condition, String message) {
		if (!condition) err(message);

		return condition;
	}

	@Override
	public boolean assertThat(boolean condition) {
		if (!condition) {
			err(new AssertionError());
		}

		return condition;
	}

	@Override
	public Instant startTime() {
		return startTime;
	}

	private final AtomicInteger runningActions = new AtomicInteger(0);
	private final Object runningActionsLock = new Object();

	@Override
	public void actionStarted() {
		runningActions.incrementAndGet();
	}

	@Override
	public void actionFinished() {
		runningActions.updateAndGet(n -> {
			n--;
			if (n == 0)
				synchronized (runningActionsLock) {
					runningActionsLock.notifyAll();
				}

			return n;
		});
	}

	@Override
	public void awaitActionCompletion() throws InterruptedException {
		synchronized (runningActionsLock) {
			if (runningActions.get() > 0)
				runningActionsLock.wait(0);
		}
	}

	public boolean actionsDone() {
		return runningActions.get() == 0;
	}

	@Override
	public CompletableFuture<Void> startActionAsync(String actionName) {
		if (!this.actions.containsKey(actionName)) {
			throw new IllegalArgumentException("Action with name " + actionName + " does not exist!");
		}

		return startActionAsync(this.actions.get(actionName));
	}

	@Override
	public CompletableFuture<Void> startActionAsync(Node.NodeTimer action) {
		var future = new CompletableFuture<Void>();
		actionExecutor.execute(() -> {
			actionStarted();
			try {
				action.start();
				future.complete(null);
			} catch (Throwable t) {
				future.completeExceptionally(t);
				sneakyThrow(t);
			} finally {
				actionFinished();
			}

		});

		return future;
	}

	@Override
	public void addAction(Node node, String actionName, Node.NodeTimer action) {
		if (this.actions.containsKey(actionName)) {
			warn("Action with name " + actionName + " already exists, can't be added!");
			return;
		}
		this.nodeActions.computeIfAbsent(node, __ -> ConcurrentHashMap.newKeySet()).add(actionName);
		this.actions.put(actionName, action);
	}

	private void unregisterAction(String actionName) {
		if (!this.actions.containsKey(actionName)) {
			warn("Action with name " + actionName + " doesn't exists, can't be unregistered!");
			return;
		}
		this.actions.remove(actionName);
	}

	public boolean nodeIsRunning(String name) {
		return nodes.containsKey(name);
	}

	public void stopAllNodes() {
		for (Node node : Set.copyOf(nodes.values())) {
			if (!node.getClass().isAnnotationPresent(Hidden.class))
				stopNode(node);
		}
	}
}
