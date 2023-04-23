package com.kuriosityrobotics.powerplay.client.logs;

import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.util.Instant;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class LoggingTopic {
    private final String displayName;
    private final Color colour;
    private final ConcurrentLinkedDeque<LoggingEntry> queue;
    private final Runnable enableListener;
    private final JLabel label;
    private boolean enabled = true;

    LoggingTopic(Orchestrator orchestrator, Runnable enableListener, Consumer<LoggingEntry> entryAddListener, String displayName, String topicName, Color colour) {
        this.displayName = displayName;
        this.colour = colour;
        this.queue = new ConcurrentLinkedDeque<>();
        this.enableListener = enableListener;
        this.label = new JLabel(displayName);
        label.setForeground(colour);

        orchestrator.subscribe(
                topicName,
                String.class,
                message -> {

                    var newEntry = new LoggingEntry(Instant.now(), LoggingTopic.this, message);
                    queue.addFirst(newEntry);
                    if (enabled) entryAddListener.accept(newEntry);
                });
    }

    public String displayName() {
        return displayName;
    }

    public Color colour() {
        return colour;
    }

    public ConcurrentLinkedDeque<LoggingEntry> queue() {
        return queue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        enableListener.run();
    }

    public JLabel label() {
        return label;
    }
}
