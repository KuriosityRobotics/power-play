package com.kuriosityrobotics.powerplay.client.telemetry;

import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.kuriosityrobotics.powerplay.client.TableColumnAdjuster;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.util.Instant;

import javax.swing.table.DefaultTableCellRenderer;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

import static com.kuriosityrobotics.powerplay.util.StringUtils.toDisplayString;

public class TelemetryGui extends SimpleToolWindowPanel {

    public TelemetryGui(Orchestrator orchestrator) {
        super(false, false);

        var model = new TelemetryTableModel(orchestrator);
        JBTable table1 = new JBTable();

        table1.setCellSelectionEnabled(true);


        table1.setModel(model);
        var renderer = new DefaultTableCellRenderer();
        table1.setDefaultRenderer(
                Instant.class,
                (table, value, a, b, c, d) -> renderer.getTableCellRendererComponent(table, value.toString(), a, b, c, d));
        table1.setDefaultRenderer(String.class, (table, value, a, b, c, d) ->
                renderer.getTableCellRendererComponent(table, toDisplayString(value), a, b, c, d)
        );

        var adjuster = new TableColumnAdjuster(table1);
        adjuster.adjustColumns();

        AtomicReference<HashSet<String>> topics = new AtomicReference<>();
        model.addTableModelListener(
                l -> {
                    if (orchestrator.getTopics().keySet().equals(topics.get())) return;

                    adjuster.adjustColumns();
					synchronized (topics) {
						topics.set(new HashSet<>(orchestrator.getTopics().keySet()));
					}
                });

        var scrollPane = new JBScrollPane(table1);
        setContent(scrollPane);
    }
}
