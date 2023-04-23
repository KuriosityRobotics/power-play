package com.kuriosityrobotics.powerplay.pubsub.bridge;

import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotAdvertisement;

import java.net.InetSocketAddress;

public class DataSocketHandlerFactory implements HandlerFactory {
    public DataSocketHandler createClient(
            Orchestrator orchestrator,
            RobotAdvertisement advertisement) {
        return new DataSocketHandler(
                orchestrator, advertisement.address(), null,  null,false);
    }

    public DataSocketHandler createServer(
            Orchestrator orchestrator,
            InetSocketAddress address) {
        return new DataSocketHandler(orchestrator, address, null, null,true);
    }
}
