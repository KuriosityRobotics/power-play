package com.kuriosityrobotics.powerplay.debug;

import static com.kuriosityrobotics.powerplay.util.StringUtils.toDisplayString;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.PriorityBlockingQueue;

public final class TelemetryTree {
	/**
	 * Format a map of telemetry topics into an ASCII art tree structure.
	 *
	 * @param topicsAndData A map of topics to their data.
	 * @return A string containing the formatted tree.
	 */
	public static String format(Collection<TopicData> topicsAndData) {
		var root = new TelemetryTree("", null);
		topicsAndData.forEach((data) -> root.getChildRecursively(data.topicName).setDatum(data.data));

		var sb = new StringBuilder();
		root.children.forEach((topLevelTopicName, topLevelTree) -> {
			topLevelTree.format(sb, true);
			sb.append("\n");
		});

		return sb.toString().strip();
	}

	private final Map<String, TelemetryTree> children = new HashMap<>();
	private final String name;
	private Object datum;

	private TelemetryTree(String name, Object datum) {
		this.name = name;
		this.datum = datum;
	}

	private TelemetryTree getChildRecursively(LinkedList<String> name) {
		if (name.isEmpty())
			return this;

		return children.computeIfAbsent(name.removeFirst(), s -> new TelemetryTree(s, null)).getChildRecursively(name);
	}

	private TelemetryTree getChildRecursively(String name) {
		return getChildRecursively(new LinkedList<>(List.of(name.split("/"))));
	}

	// Format the tree
	private void format(StringBuilder sb, boolean isRoot) {
		if(Objects.equals(name, "gamepad2") || Objects.equals(name, "gamepad1")) return;
		sb.append(isRoot ? "|- " : "/");
		sb.append(name);

		if (datum != null)
			sb.append(" = ").append(toDisplayString(datum));

		for (var child : children.values()) {
			boolean currentLineBreaks = datum != null || children.size() > 1;

			var childSb = new StringBuilder();
			child.format(childSb, false);
			var childLines = childSb.toString().split("\n");
			for (int i = 0; i < childLines.length; i++) {
				String line = childLines[i];

				if (currentLineBreaks) {
					if (i == 0) {
						sb.append("\n|--- ")
							.append(line, 1, line.length());
					} else {
						sb.append("\n  ").append(line);
					}
				} else {
					if (i != 0)
						sb.append("\n").append("-".repeat(name.length() + 1));

					sb.append(line);
				}

			}
		}
	}

	private void setDatum(Object datum) {
		this.datum = datum;
	}
}


