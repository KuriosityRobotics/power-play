package com.kuriosityrobotics.powerplay.client.logs;

import com.intellij.openapi.options.advanced.AdvancedSettingBean;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.util.Instant;

import javax.swing.*;

class LoggingEntry {

    private final Instant time;
    private final LoggingTopic parentTopic;
    private final String message;

    public LoggingEntry(Instant time, LoggingTopic parentTopic, String message) {
        this.time = time;
        this.parentTopic = parentTopic;
        this.message = message;
    }

    public Instant time() {
        return time;
    }


    public String message() {
        return message;
    }

    public LoggingTopic parentTopic() {
        return parentTopic;
    }
}
