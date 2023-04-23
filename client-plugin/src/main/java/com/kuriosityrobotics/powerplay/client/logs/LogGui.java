package com.kuriosityrobotics.powerplay.client.logs;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.util.Instant;
import com.kuriosityrobotics.powerplay.client.TableColumnAdjuster;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class LogGui extends SimpleToolWindowPanel {

    public LogGui(Orchestrator orchestrator) {
        super(false, false);

        LoggingTableModel model = new LoggingTableModel(orchestrator);
        List<ToggleButton> topicButtons = new LinkedList<>();
        for (var topic : model.topics()) {
            topicButtons.add(new ToggleButton(topic.displayName(), topic::setEnabled, topic.isEnabled()));
        }

        var toolbar =
                ActionManager.getInstance()
                        .createActionToolbar
                                (
                                        "Logging",
                                        ToggleButton.createActionGroup(topicButtons.toArray(ToggleButton[]::new)),
                                        false
                                );
        toolbar.adjustTheSameSize(true);
        toolbar.setMinimumButtonSize(new Dimension(50, 20));
        toolbar.setTargetComponent(this);
        add(toolbar.getComponent(), BorderLayout.WEST);


        var wordWrapRenderer = new MultilineTableCell();
        JTable table1 = new JBTable() {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (model.getColumnClass(column) == String.class ) {
                    return wordWrapRenderer;
                }
                else {
                    return super.getCellRenderer(row, column);
                }
            }
        };

        table1.setCellSelectionEnabled(true);
        table1.setModel(model);

        table1.setRowSorter(new TableRowSorter<>(model));

        TableCellRenderer jLabelRendered = (table, value, isSelected, hasFocus, row, column) -> {
            var v = (JLabel) value;
            v.setOpaque(true);
            if (isSelected)
                v.setBackground(table.getSelectionBackground());
            else
                v.setBackground(table.getBackground());
            return v;
        };
        table1.setDefaultRenderer(JLabel.class, jLabelRendered);
        table1.setDefaultRenderer(
                Instant.class,
                (table, value, isSelected, hasFocus, c, d) -> jLabelRendered.getTableCellRendererComponent(table, new JLabel(value.toString()), isSelected, hasFocus, c, d));

        // make table fill the whole window
        JScrollPane scrollPane = new JBScrollPane(table1);
        add(scrollPane);

        var adjuster = new TableColumnAdjuster(table1);
        adjuster.setDynamicAdjustment(true);
        model.addTableModelListener(adjuster);
        adjuster.adjustColumns();

    }

}
