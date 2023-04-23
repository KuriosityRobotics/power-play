package com.kuriosityrobotics.powerplay.pubsub.bridge;

import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class TestDataSocketHandler extends DataSocketHandler {
    protected TestDataSocketHandler(Orchestrator orchestrator, InetSocketAddress address, Consumer<RobotDetails> onReceivedRobotDetails, Runnable onDisconnected, boolean isServer) {
        super(orchestrator, address, onReceivedRobotDetails, onDisconnected, isServer);
    }

    @Override
    void send(String topicName, byte[] data) {
        super.send(topicName, data);
        // emulate 500kb/s transfer speed using thread.sleep
        try {
            Thread.sleep(1000L * data.length / 500_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
