package com.kuriosityrobotics.powerplay.pubsub.bridge;

import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotAdvertisement;

import java.net.InetSocketAddress;

public class TestDataSocketHandlerFactory implements HandlerFactory{
    public DataSocketHandler createClient(
            Orchestrator orchestrator,
            RobotAdvertisement advertisement) {
        return new TestDataSocketHandler(
                orchestrator, advertisement.address(), null,  null,false);
    }

    public DataSocketHandler createServer(
            Orchestrator orchestrator,
            InetSocketAddress address) {
        return new TestDataSocketHandler(orchestrator, address, null, null,true);
    }
}
