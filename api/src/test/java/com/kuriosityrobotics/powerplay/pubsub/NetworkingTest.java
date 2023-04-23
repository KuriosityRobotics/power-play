package com.kuriosityrobotics.powerplay.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static java.lang.Thread.sleep;

import com.kuriosityrobotics.powerplay.pubsub.bridge.BidirectionalBridge;
import com.kuriosityrobotics.powerplay.pubsub.bridge.TestDataSocketHandlerFactory;
import com.kuriosityrobotics.powerplay.util.Instant;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class NetworkingTest {
    @BeforeEach
    void setUp() throws InterruptedException {
        BidirectionalBridge.ADVERTISE_PORT = new Random(0).nextInt(1000) + 1000;
    }

    void startTestBridge(LogInterface _logInterface) {
        var orchestrator = (OrchestratorImpl) _logInterface;
        orchestrator.bridge = new BidirectionalBridge(orchestrator, new TestDataSocketHandlerFactory());
        orchestrator.startNode("bridge", orchestrator.bridge);
    }

    @RepeatedTest(5)
    void testDiscovery() {
        try (var orchestrator = Orchestrator.createTest("test", false);
             var secondOrchestrator = Orchestrator.createTest("test2", false)) {
            assertEquals(0, orchestrator.connectedRobots().size());
            assertEquals(0, secondOrchestrator.connectedRobots().size());

            startTestBridge(orchestrator);
            assertEquals(0, orchestrator.connectedRobots().size());

            startTestBridge(secondOrchestrator);
            var start = Instant.now();
            while (orchestrator.connectedRobots().size() < 1
                    || secondOrchestrator.connectedRobots().size() < 1) {
                if (Instant.now().isAfter(start.plusSeconds(1))) {
                    fail(
                            "Timed out waiting for discovery.  "
                                    + "orchestrator.connectedRobots().size() = "
                                    + orchestrator.connectedRobots().size()
                                    + " secondOrchestrator.connectedRobots().size() = "
                                    + secondOrchestrator.connectedRobots().size());
                }
            }

            assertEquals(1, orchestrator.connectedRobots().size());
            assertEquals(1, secondOrchestrator.connectedRobots().size());

            // A second bridge on the same node should not cause a new robot to be discovered
            secondOrchestrator.startNode(
                    "bridge2",
                    new BidirectionalBridge(secondOrchestrator));
            // wait a bit
            start = Instant.now();
            while (Instant.now().isBefore(start.plusMillis(300))) {
                assertEquals(Set.of(secondOrchestrator.robotDetails()), orchestrator.connectedRobots(), "Too many robots connected");
                assertEquals(Set.of(orchestrator.robotDetails()), secondOrchestrator.connectedRobots(), "Too many robots connected");
            }

            assertEquals(1, orchestrator.connectedRobots().size());
            assertEquals(1, secondOrchestrator.connectedRobots().size());

            secondOrchestrator.stopNode("bridge2");
        }
    }

    @Test
    @Disabled
        // this always failed and is literally never going to be relevant
    void testManyRobots() throws InterruptedException {
        try (var orchestrator = Orchestrator.createTest("test", false);
             var secondOrchestrator = Orchestrator.createTest("test2", false)) {
            var orchestrators = new Vector<Orchestrator>();
            orchestrators.add(orchestrator);
            orchestrators.add(secondOrchestrator);

            startTestBridge(orchestrator);
            startTestBridge(secondOrchestrator);
            for (int i = 5; i < 10; i++) {
                var newOrchestrator = Orchestrator.createTest("test" + i, true);
                orchestrators.add(newOrchestrator);
            }
            sleep(1000);
            for (var orch : orchestrators) {
                assertEquals(
                        orchestrators.stream()
                                .map(Orchestrator::robotDetails)
                                .filter(deets -> !deets.equals(orch.robotDetails()))
                                .collect(Collectors.toSet()),
                        orch.connectedRobots());
            }
            var subscriptionResults = new Vector<Object[]>();
            for (var orch : orchestrators)
                orch.subscribe(
                        "testTopic",
                        String.class,
                        (datum, topicName, robotDetails) ->
                                subscriptionResults.add(
                                        new Object[]{topicName, datum, robotDetails}));

            orchestrator.dispatch("testTopic", "test");
            sleep(1000);
            assertEquals(orchestrators.size(), subscriptionResults.size());
            for (var result : subscriptionResults) {
                assertEquals("testTopic", result[0]);
                assertEquals("test", result[1]);
                assertEquals(orchestrator.robotDetails(), result[2]);
            }

            orchestrators.remove(orchestrator);
            orchestrators.remove(secondOrchestrator);
            orchestrators.forEach(Orchestrator::close);
        }
    }

    @Test
    void testHighFrequency() throws InterruptedException {
        try (var orchestrator = Orchestrator.createTest("test", false);
             var secondOrchestrator = Orchestrator.createTest("test2", false)) {
            orchestrator.startBridge();
            secondOrchestrator.startBridge();

            while (orchestrator.connectedRobots().size() < 1
                    || secondOrchestrator.connectedRobots().size() < 1) Thread.onSpinWait();

            var receivedMessages = new Vector<String>();

            secondOrchestrator.subscribe("test", String.class, n -> receivedMessages.add(n));
            orchestrator.setBlockingDispatch(false);
            secondOrchestrator.setBlockingDispatch(false);
            for (int i = 0; i < 500; i++) {
                orchestrator.dispatch("test", "test");
            }

            Thread.sleep(500);
            assertEquals(500, receivedMessages.size());

            for (String receivedMessage : receivedMessages) assertEquals("test", receivedMessage);
        }
    }

    @Test
    void findLatency() throws InterruptedException {
        try (var orchestrator = Orchestrator.createTest("test", false);
             var secondOrchestrator = Orchestrator.createTest("test2", false)) {
            startTestBridge(orchestrator);
            startTestBridge(secondOrchestrator);
            orchestrator.setBlockingDispatch(false);
            secondOrchestrator.setBlockingDispatch(false);
            while (orchestrator.connectedRobots().size() < 1
                    || secondOrchestrator.connectedRobots().size() < 1) Thread.onSpinWait();
            orchestrator.dispatch("test", "test");
            Thread.sleep(1000);
            var sentMessages = new Vector<Instant>();
            var receivedMessages = new Vector<Instant>();

            secondOrchestrator.subscribe("test", String.class, n -> receivedMessages.add(Instant.now()));
            var message = new byte[10_000];
            for (int i = 0; i < 1000; i++) {
                if (i % 10 == 0) {
                    sentMessages.add(Instant.now());
                    orchestrator.dispatch("test", "test");
                }
                if (i % 20 == 0)
                    orchestrator.dispatch("fsddfs", message);
            }

            while (receivedMessages.size() < sentMessages.size()) Thread.onSpinWait();
            // Find the min, max, median and mean latency
            var latencies = new double[sentMessages.size()];
            for (int i = 0; i < sentMessages.size(); i++) {
                latencies[i] = sentMessages.get(i).until(receivedMessages.get(i)).toMillis();
            }

            var min = StatUtils.min(latencies);
            var max = StatUtils.max(latencies);
            var median = StatUtils.percentile(latencies, 50);
            var mean = StatUtils.mean(latencies);
            System.out.printf("Min: %f, Max: %f, Median: %f, Mean: %f%n", min, max, median, mean);
        }
    }

    @RepeatedTest(10)
    void testLargeSize() throws InterruptedException {
        try (var orchestrator = Orchestrator.createTest("test", false);
             var secondOrchestrator = Orchestrator.createTest("test2", false)) {
            var message = new byte[1_000_000];

            startTestBridge(orchestrator);
            startTestBridge(secondOrchestrator);

            while (orchestrator.connectedRobots().size() < 1
                    || secondOrchestrator.connectedRobots().size() < 1) Thread.onSpinWait();

            var receivedMessages = new Vector<byte[]>();
            secondOrchestrator.subscribe("test", byte[].class, (Consumer<byte[]>) receivedMessages::add);
            orchestrator.dispatch("test", message);

            // Wait up to 5 seconds for the message to be received
            var start = Instant.now();
            while (receivedMessages.size() < 1
                    && Instant.now().isBefore(start.plusSeconds(5)))
                Thread.onSpinWait();

            assertEquals(1, receivedMessages.size());
            assertEquals(Arrays.hashCode(message), Arrays.hashCode(receivedMessages.get(0)));
        }
    }
}
