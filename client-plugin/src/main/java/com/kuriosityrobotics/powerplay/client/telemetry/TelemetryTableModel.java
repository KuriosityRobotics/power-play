package com.kuriosityrobotics.powerplay.client.telemetry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Topics;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import com.kuriosityrobotics.powerplay.util.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.jetbrains.annotations.Nls;

public class TelemetryTableModel implements TableModel {
   private final Orchestrator orchestrator;
   private final Set<TableModelListener> listeners;
   private final BiMap<Integer, Topics.RobotTopic<?>> rowIndices =
		   Maps.synchronizedBiMap(HashBiMap.create());
   private Set<String> lastTopics;
   private final int lastRowCount = 0;

   TelemetryTableModel(Orchestrator orchestrator) {
	  this.orchestrator = orchestrator;

	  this.listeners = ConcurrentHashMap.newKeySet();

	  orchestrator.subscribeToPattern(
			  Pattern.compile(".*"), // this excludes topics starting with telemetry/
			  (message, name, details) -> {
				 TableModelEvent event;
				 var index =
						 rowIndices.inverse().get(new Topics.RobotTopic<>(name, null, details));
				 if (index != null)
					event =
							new TableModelEvent(
									this,
									index,
									index,
									TableModelEvent.ALL_COLUMNS,
									TableModelEvent.UPDATE);
				 else {
					rowIndices.clear();
					event = new TableModelEvent(this);
				 }
				 listeners.forEach(n -> n.tableChanged(event));
			  });
   }

   public synchronized int getRowCount() {
	  return (int)
			  Topics.getMatchingTopicEntries(
							  orchestrator.getTopics(),
							  ignored -> true,
							  ignored -> true)
					  .count();
   }

   @Override
   public int getColumnCount() {
	  return 4;
   }

   @Nls
   @Override
   public String getColumnName(int columnIndex) {
	  switch (columnIndex) {
		 case 0:
			return "Topic name";
		 case 1:
			return "Robot";
		 case 2:
			return "Last time published";
		 case 3:
			return "Last value";
	  }

	  throw new IllegalArgumentException("Only 3 columns");
   }

   @Override
   public Class<?> getColumnClass(int columnIndex) {
	  switch (columnIndex) {
		 case 0:
		 case 3:
			return String.class;
		 case 1:
			return RobotDetails.class;
		 case 2:
			return Instant.class;
	  }

	  throw new IllegalArgumentException("Only 4 columns");
   }

   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
	  return false;
   }

   @Override
   public synchronized Object getValueAt(int rowIndex, int columnIndex) {
	  var row =
			  rowIndices.computeIfAbsent(
					  rowIndex,
					  index -> {
						 var value =
								 Topics.getMatchingTopicEntries(
												 orchestrator.getTopics(),
												 ignored -> true,
												 ignored -> true)
										 .skip(index)
										 .findFirst()
										 .orElseThrow();
						 if (rowIndices.containsValue(value)) rowIndices.clear();

						 return value;
					  });

	  switch (columnIndex) {
		 case 0:
			return row.topicName();
		 case 1:
			return row.details();
		 case 2:
			return row.lastValueTime();
		 case 3:
			return row.lastValue();
	  }
	  throw new Error("Only 4 columns");
   }

   @Override
   public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	  throw new UnsupportedOperationException();
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
