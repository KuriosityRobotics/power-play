package com.kuriosityrobotics.powerplay.pubsub.bridge.queue;

import org.jetbrains.annotations.NotNull;

public class RoundRobinTopic implements Comparable<RoundRobinTopic> {
    private final byte[] data;
    private final String topicName;
    private final int bytesWritten;


    RoundRobinTopic(byte[] data, String topicName, int bytesWritten) {
        this.data = data;
        this.topicName = topicName;
        this.bytesWritten = bytesWritten;
    }


    public int getBytesWritten() {
        return bytesWritten;
    }

    public String getTopicName() {
        return topicName;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public int compareTo(RoundRobinTopic o) {
        if (this.topicName.equals(o.topicName))
            return -Long.compare(this.bytesWritten, o.bytesWritten);

        return Long.compare(this.bytesWritten, o.bytesWritten);
    }

    @Override
    public String toString() {
        return "RoundRobinTopic{" +
                "topicName='" + topicName + '\'' +
                ", bytesWritten=" + bytesWritten +
                '}';
    }
}
