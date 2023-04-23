package com.kuriosityrobotics.powerplay.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.util.Duration;
import com.kuriosityrobotics.powerplay.util.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class OrchestratorTest {
	private static final RobotDetails ROBOT_DETAILS = new RobotDetails("test", 0);
	private static final RobotDetails SECOND_ROBOT_DETAILS = new RobotDetails("test2", 1);
	private Orchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator = Orchestrator.createTest(ROBOT_DETAILS, false);
		orchestrator.setBlockingDispatch(true);
	}

	@AfterEach
	void tearDown() {
		orchestrator.close();
	}

	private static void assertBlocksFor(Duration timeout, Runnable runnable) throws InterruptedException {
		var thread = new Thread(runnable);
		thread.start();
		Thread.sleep(timeout.toMillis());
		assertTrue(thread.isAlive());
		thread.stop();
	}

	@Test
	void waitForMessage() throws InterruptedException {
		orchestrator.startNode("test", new Node(orchestrator) {
			@RunPeriodically(maxFrequency = 1)
			public void publishMessage() {
				orchestrator.dispatch("test", "hello");
			}
		});
		Thread.sleep(100);
		assertEquals("hello", orchestrator.waitForMessage("test"));
		assertBlocksFor(Duration.ofMillis(100), () -> {
			try {
				orchestrator.waitForMessage("test");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});

		AtomicReference<String> message = new AtomicReference<>();
		var t = new Thread(() -> {
			try {
				message.set(orchestrator.waitForMessage("test"));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		t.start();
		Thread.sleep(100);
		assertTrue(t.isAlive());
		orchestrator.dispatch("test", "hello2");
		Thread.sleep(100);
		assertFalse(t.isAlive());
		assertEquals("hello2", message.get());
	}

	@Test
	void dispatch() {
		orchestrator.dispatch("testTopic", SECOND_ROBOT_DETAILS, "testMessage");
		assertEquals(
			"testMessage",
			orchestrator
				.getTopic("testTopic")
				.orElseThrow()
				.lastValues()
				.get(SECOND_ROBOT_DETAILS));

		orchestrator.dispatch("testTopic", ROBOT_DETAILS, "testMessage2");
		assertEquals(
			"testMessage",
			orchestrator
				.getTopic("testTopic")
				.orElseThrow()
				.lastValues()
				.get(SECOND_ROBOT_DETAILS));
		assertEquals(
			"testMessage2",
			orchestrator.getTopic("testTopic").orElseThrow().lastValues().get(ROBOT_DETAILS));

		orchestrator.dispatch("testTopic", SECOND_ROBOT_DETAILS, "testMessage3");
		assertEquals(
			"testMessage2",
			orchestrator.getTopic("testTopic").orElseThrow().lastValues().get(ROBOT_DETAILS));
		assertEquals(
			"testMessage3",
			orchestrator
				.getTopic("testTopic")
				.orElseThrow()
				.lastValues()
				.get(SECOND_ROBOT_DETAILS));

		orchestrator.dispatch("testTopic", ROBOT_DETAILS, "testMessage4");
		assertEquals(
			"testMessage3",
			orchestrator
				.getTopic("testTopic")
				.orElseThrow()
				.lastValues()
				.get(SECOND_ROBOT_DETAILS));
		assertEquals(
			"testMessage4",
			orchestrator.getTopic("testTopic").orElseThrow().lastValues().get(ROBOT_DETAILS));

		assertThrows(
			IllegalArgumentException.class,
			() -> orchestrator.dispatch(null, ROBOT_DETAILS, "testMessage5"),
			"Topic name cannot be null");
		assertThrows(
			IllegalArgumentException.class,
			() -> orchestrator.dispatch("testTopic", null, "testMessage6"),
			"Originating robot cannot be null");
		assertThrows(
			IllegalArgumentException.class,
			() -> orchestrator.dispatch(null, ROBOT_DETAILS, "testTopic"),
			"Message cannot be null");
	}

	@Test
	void listenForAny() {
		var triggered = new AtomicBoolean(false);
		orchestrator.listenForAny(() -> triggered.set(true), "testTopic", "testTopic2");
		orchestrator.dispatch("testTopic", SECOND_ROBOT_DETAILS, "testMessage");

		assertTrue(triggered.get());

		triggered.set(false);
		orchestrator.dispatch("testTopic2", SECOND_ROBOT_DETAILS, "testMessage");
		assertTrue(triggered.get());

		triggered.set(false);
		orchestrator.dispatch("testTopic", ROBOT_DETAILS, "testMessage");
		assertTrue(triggered.get());

		triggered.set(false);
		orchestrator.dispatch("testTopic2", ROBOT_DETAILS, "testMessage");
		assertTrue(triggered.get());
	}

	@Test
	void subscribe() {
		var lastTopic = new AtomicReference<>();
		var lastMessage = new AtomicReference<>();
		var lastRobot = new AtomicReference<>();
		orchestrator.subscribe(
			"testTopic",
			String.class,
			(message, topicName, robot) -> {
				lastTopic.set(topicName);
				lastMessage.set(message);
				lastRobot.set(robot);
			});
		orchestrator.dispatch("testTopic", SECOND_ROBOT_DETAILS, "testMessage");
		assertEquals("testTopic", lastTopic.get());
		assertEquals("testMessage", lastMessage.get());
		assertEquals(SECOND_ROBOT_DETAILS, lastRobot.get());

		lastTopic.set(null);
		lastMessage.set(null);
		lastRobot.set(null);
		orchestrator.dispatch("testTopic", ROBOT_DETAILS, "testMessage");
		assertEquals("testTopic", lastTopic.get());
		assertEquals("testMessage", lastMessage.get());
		assertEquals(ROBOT_DETAILS, lastRobot.get());

		lastTopic.set(null);
		lastMessage.set(null);
		lastRobot.set(null);
		orchestrator.dispatch("testTopic2", SECOND_ROBOT_DETAILS, "testMessage");
		assertNull(lastTopic.get());
		assertNull(lastMessage.get());
		assertNull(lastRobot.get());

		lastTopic.set(null);
		lastMessage.set(null);
		lastRobot.set(null);
		orchestrator.dispatch("testTopic", ROBOT_DETAILS, "testMessage");
		assertEquals("testTopic", lastTopic.get());
		assertEquals("testMessage", lastMessage.get());
		assertEquals(ROBOT_DETAILS, lastRobot.get());
	}

	@Test
	void publisher() {
		var lastTopic = new AtomicReference<>();
		var lastMessage = new AtomicReference<>();
		var lastRobot = new AtomicReference<>();
		var publisher = orchestrator.publisher("testTopic", String.class);
		orchestrator.subscribe(
			"testTopic",
			String.class,
			(message, topicName, robot) -> {
				lastTopic.set(topicName);
				lastMessage.set(message);
				lastRobot.set(robot);
			});
		publisher.publish("testMessage");
		assertEquals("testTopic", lastTopic.get());
		assertEquals("testMessage", lastMessage.get());
		assertEquals(ROBOT_DETAILS, lastRobot.get());

		lastTopic.set(null);
		lastMessage.set(null);
		lastRobot.set(null);
		assertThrows(
			IllegalArgumentException.class,
			() -> publisher.publish(null),
			"Message cannot be null");
		assertNull(lastTopic.get());
		assertNull(lastMessage.get());
		assertNull(lastRobot.get());

		lastTopic.set(null);
		lastMessage.set(null);
		lastRobot.set(null);

//		assertThrows(
//				IllegalArgumentException.class,
//				() -> orchestrator.publisher("testTopic", Integer.class));
		assertNull(lastTopic.get());
		assertNull(lastMessage.get());
		assertNull(lastRobot.get());
	}

	@Test
	void lastValue() {
		var firstLastValue = orchestrator.lastValue("testTopic", String.class, ROBOT_DETAILS);
		var secondLastValue =
			orchestrator.lastValue("testTopic", String.class, SECOND_ROBOT_DETAILS);
		var bothLastValue = orchestrator.lastValue("testTopic", String.class);

		assertNull(firstLastValue.getValue());
		assertNull(secondLastValue.getValue());
		assertNull(bothLastValue.getValue());

		firstLastValue.setDefaultValue("defaultValue");
		assertEquals("defaultValue", firstLastValue.getValue());
		assertNull(secondLastValue.getValue());
		assertNull(bothLastValue.getValue());

		secondLastValue.setDefaultValue("defaultValue");
		assertEquals("defaultValue", firstLastValue.getValue());
		assertEquals("defaultValue", secondLastValue.getValue());
		assertNull(bothLastValue.getValue());

		bothLastValue.setDefaultValue("defaultValue");
		assertEquals("defaultValue", firstLastValue.getValue());
		assertEquals("defaultValue", secondLastValue.getValue());
		assertEquals("defaultValue", bothLastValue.getValue());

		orchestrator.dispatch("testTopic", ROBOT_DETAILS, "testMessage");
		assertEquals("testMessage", firstLastValue.getValue());
		assertEquals("defaultValue", secondLastValue.getValue());
		assertEquals("testMessage", bothLastValue.getValue());

		orchestrator.dispatch("testTopic", SECOND_ROBOT_DETAILS, "testMessage2");
		assertEquals("testMessage", firstLastValue.getValue());
		assertEquals("testMessage2", secondLastValue.getValue());
		assertEquals("testMessage2", bothLastValue.getValue());
	}

	@RepeatedTest(5)
	void node() throws InterruptedException {
		AtomicBoolean innerClassUpdatedTooFast = new AtomicBoolean(false);
		AtomicInteger updateCount = new AtomicInteger(0);
		var node =
			new Node(orchestrator) {
				private final AtomicLong lastUpdate = new AtomicLong();

				@RunPeriodically(maxFrequency = 100)
				public void update() {
					updateCount.incrementAndGet();
					var currentTime = Instant.now().toEpochMilli();
					innerClassUpdatedTooFast.set((currentTime - lastUpdate.get()) < 9);
					lastUpdate.set(currentTime);
				}
			};

		orchestrator.startNode("testNode", node);
		Thread.sleep(30);
		assertFalse(innerClassUpdatedTooFast.get());

		orchestrator.stopNode("testNode");
		Thread.sleep(30);
		int startCount = updateCount.get();
		Thread.sleep(30);
		assertEquals(startCount, updateCount.get());
	}

	@Test
	void getTopics() {
		var topics = orchestrator.getTopics();

		var startSize = topics.size();
		orchestrator.dispatch("testTopic", ROBOT_DETAILS, "testMessage");
		topics = orchestrator.getTopics();
		assertEquals(startSize + 1, topics.size());
		assertNotNull(topics.get("testTopic"));
	}

	@Test
	void getPatternMatchingTopicByIndex() {
	}

	@Test
	void removeSubscription() {
	}
}
