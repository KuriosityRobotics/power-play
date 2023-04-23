//package com.kuriosityrobotics.powerplay.util;
//
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import static java.lang.Math.abs;
//
//import com.kuriosityrobotics.powerplay.pubsub.Node;
//import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.RepeatedTest;
//
//public class NodeTimerTest {
//	private static final int TEST_THRESHOLD_MILLIS = 20;
//	private TestNode node;
//
//	@BeforeEach
//	void setup() {
//		var orchestrator = Orchestrator.createTest("robot", false);
//		node = new TestNode(orchestrator);
//		orchestrator.startNode("timer", node);
//	}
//
//	@RepeatedTest(10)
//	void noDelayTest() throws InterruptedException {
//		node.noDelayTest();
//
//		synchronized (node) {
//			node.wait(2000);
//		}
//
//		assertTrue(abs(node.finishTime.since(node.startTime).toMillis()) < TEST_THRESHOLD_MILLIS);
//	}
//
//	@RepeatedTest(10)
//	void fancyNoDelayTest() throws InterruptedException {
//		node.fancyNoDelayTest();
//
//		synchronized (node) {
//			node.wait(2000);
//		}
//
//		assertTrue(abs(node.finishTime.since(node.startTime).toMillis()) < TEST_THRESHOLD_MILLIS);
//	}
//
//	@RepeatedTest(10)
//	void oneDelayTest() throws InterruptedException {
//		var delayMills = 100;
//		node.oneDelayTest(Duration.ofMillis(delayMills));
//
//		synchronized (node) {
//			node.wait(2000);
//		}
//
//		assertTrue(abs(node.firstActionTime.since(node.startTime).toMillis() - delayMills) < TEST_THRESHOLD_MILLIS);
//	}
//
//	@RepeatedTest(10)
//	void twoDelayTest() throws InterruptedException {
//		var delay1Mills = 200;
//		var delay2Mills = 300;
//
//		node.twoDelayTest(Duration.ofMillis(delay1Mills), Duration.ofMillis(delay2Mills));
//
//		synchronized (node) {
//			node.wait(2000);
//		}
//
//		assertTrue(abs(node.firstActionTime.since(node.startTime).toMillis() - delay1Mills) < TEST_THRESHOLD_MILLIS);
//		assertTrue(abs(node.secondActionTime.since(node.startTime).toMillis() - (delay1Mills + delay2Mills)) < TEST_THRESHOLD_MILLIS);
//		assertTrue(abs(node.secondActionTime.since(node.firstActionTime).toMillis() - delay2Mills) < TEST_THRESHOLD_MILLIS);
//	}
//
//	protected static class TestNode extends Node {
//		Instant startTime, finishTime, firstActionTime, secondActionTime;
//
//		protected TestNode(Orchestrator orchestrator) {
//			super(orchestrator);
//		}
//
//		void noDelayTest() {
//			startTime = Instant.now();
//			interruptibleTimer()
//				.delayThenRun(Duration.ofMillis(0), () -> {
//					finishTime = Instant.now();
//					synchronized (this) {
//						notify();
//					}
//				})
//				.start();
//		}
//
//		void fancyNoDelayTest() {
//			interruptibleTimer()
//				.delayThenRun(Duration.ofMillis(0), () -> startTime = Instant.now())
//				.delayThenRun(Duration.ofMillis(0), () -> {
//					finishTime = Instant.now();
//					synchronized (this) {
//						notify();
//					}
//				})
//				.start();
//		}
//
//		void oneDelayTest(Duration delay) {
//			startTime = Instant.now();
//			interruptibleTimer()
//				.delayThenRun(delay, () -> {
//					firstActionTime = Instant.now();
//					synchronized (this) {
//						notify();
//					}
//				})
//				.start();
//		}
//
//		void twoDelayTest(Duration delay1, Duration delay2) {
//			startTime = Instant.now();
//			interruptibleTimer()
//				.delayThenRun(delay1, () -> firstActionTime = Instant.now())
//				.delayThenRun(delay2, () -> {
//					secondActionTime = Instant.now();
//					synchronized (this) {
//						notify();
//					}
//				})
//				.start();
//		}
//	}
//}
