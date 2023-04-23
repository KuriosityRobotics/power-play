package com.kuriosityrobotics.powerplay.pubsub.bridge.queue;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoundRobinQueueTest {
    @Test
    void testOrder() {
        var queue = new RoundRobinQueue();
        for (int i = 1; i < 100; i++) {
            queue.add(new RoundRobinTopic(new byte[i], "" + i, i));
        }

        byte[] prev = new byte[0];
        while (queue.hasNext()) {
            var next = queue.next();
            assertTrue(prev.length <= next.length);

            prev = next;
        }
    }

    @Test
    void testIntraTopicOrdering() {
        var queue = new RoundRobinQueue();
        for (int i = 1; i < 100; i++) {
            queue.add(new RoundRobinTopic(new byte[i], "af", i));
        }

        byte[] prev = new byte[101];
        while (queue.hasNext()) {
            var next = queue.next();
            assertTrue(prev.length >= next.length);

            prev = next;
        }
    }

}