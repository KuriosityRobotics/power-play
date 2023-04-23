package com.kuriosityrobotics.powerplay.client.logs;

import com.intellij.ui.JBColor;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.kuriosityrobotics.powerplay.util.Instant;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LoggingTableModel implements TableModel {
    private static final int MAX_ENTRIES = 200;
    private final Set<TableModelListener> listeners;

    private final List<LoggingTopic> topics;
    private ConcurrentLinkedDeque<LoggingEntry> entries;

	LoggingTableModel(Orchestrator orchestrator) {
		this.listeners = new HashSet<>();

        this.entries = new ConcurrentLinkedDeque<>();

        this.topics = List.of(
                new LoggingTopic(orchestrator, this::updateMessageQueue, this::onNewVisibleEntry, "Debug", "log/debug", JBColor.BLUE),
                new LoggingTopic(orchestrator, this::updateMessageQueue, this::onNewVisibleEntry, "Info", "log/info", JBColor.GREEN),
                new LoggingTopic(orchestrator, this::updateMessageQueue, this::onNewVisibleEntry, "Warn", "log/warn", JBColor.ORANGE),
                new LoggingTopic(orchestrator, this::updateMessageQueue, this::onNewVisibleEntry, "Error", "log/error", JBColor.RED)
        );
    }

    private void onNewVisibleEntry(LoggingEntry entry) {
        var mostRecentMessageOfTopic = entries.parallelStream().filter(e -> e.parentTopic().equals(entry.parentTopic())).findFirst().orElse(null);
        if (mostRecentMessageOfTopic != null && mostRecentMessageOfTopic.message().equals(entry.message())) {
            entries.remove(mostRecentMessageOfTopic);
        }

        entries.addFirst(entry);

        while (entries.size() > MAX_ENTRIES) {
            entries.removeLast().parentTopic().queue().removeLast();
        }

        for (var listener : listeners) {
            listener.tableChanged(new TableModelEvent(this, 0, 0, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
        }
    }

   private void updateMessageQueue() {
        var newEntries = new LinkedList<LoggingEntry>();
        for (var topic : topics) {
            if (topic.isEnabled())
                newEntries.addAll(topic.queue());
        }

        newEntries.sort(Comparator.comparing(LoggingEntry::time).reversed());
        this.entries = new ConcurrentLinkedDeque<>(newEntries);
		for (var listener : listeners) {
			listener.tableChanged(new TableModelEvent(this));
		}
    }

	public List<LoggingTopic> topics() {
		return topics;
	}

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Nls
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "Time";
            case 1:
                return "Type";
            case 2:
                return "Message";
            default:
                return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Instant.class;
            case 1:
                return JLabel.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var iterator = entries.iterator();
		for (int i = 0; i < rowIndex - 1; i++) {
			iterator.next();
		}
		var row = iterator.next();
        if (columnIndex == 0) return row.time();
        else if (columnIndex == 1) return row.parentTopic().label();
        else if (columnIndex == 2) return row.message();
        else return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Editing is not supported");
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

}
