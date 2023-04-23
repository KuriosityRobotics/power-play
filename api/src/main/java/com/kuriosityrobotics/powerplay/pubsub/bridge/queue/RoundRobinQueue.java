package com.kuriosityrobotics.powerplay.pubsub.bridge.queue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinQueue {
    private final ConcurrentHashMap<String, AtomicInteger> topicNames;
    private final PriorityBlockingQueue<RoundRobinTopic> queue;

    public RoundRobinQueue() {
        this.topicNames = new ConcurrentHashMap<>();
        this.queue = new PriorityBlockingQueue<>();
    }

    public void add(String topicName, byte[] message) {
        var bytesWritten = topicNames.computeIfAbsent(topicName, caption -> new AtomicInteger(0));
        var rr = new RoundRobinTopic(message, topicName, bytesWritten.get());
        queue.add(rr);
    }

    public void add(RoundRobinTopic rr) {
        topicNames.computeIfAbsent(rr.getTopicName(), caption -> new AtomicInteger(rr.getBytesWritten()));
        queue.add(rr);
    }

    public boolean hasNext() {
        return !queue.isEmpty();
    }

    public byte[] next() {
        var result = queue.poll();
        if (result == null)
            return null;
        topicNames.get(result.getTopicName()).addAndGet(result.getData().length);
        return result.getData();
    }

}
