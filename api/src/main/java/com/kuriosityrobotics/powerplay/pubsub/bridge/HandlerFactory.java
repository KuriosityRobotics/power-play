package com.kuriosityrobotics.powerplay.pubsub.bridge;

import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotAdvertisement;

import java.net.InetSocketAddress;

interface HandlerFactory {
    DataSocketHandler createClient(
            Orchestrator orchestrator,
            RobotAdvertisement advertisement);

    DataSocketHandler createServer(
            Orchestrator orchestrator,
            InetSocketAddress address);
}
