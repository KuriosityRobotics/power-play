package com.kuriosityrobotics.powerplay.debug;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.robocol.Command;
import com.qualcomm.robotcore.robocol.TelemetryMessage;

import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.RobotCoreCommandList;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Pattern;

@Hidden
public final class DriverHubTelemetry extends Node {
	private static final int MAX_LOG_ENTRIES = 40;

	private final OpModeManagerImpl opModeManager;

	// display logs & everything else separately
	// private final Map<String, Object> topicData = Collections.synchronizedMap(new LinkedHashMap<>());
	private final PriorityBlockingQueue<TopicData> topicDataList;
	private final ConcurrentHashMap<String, TopicData> topicData;
	private final ConcurrentLinkedDeque<String> logs = new ConcurrentLinkedDeque<>();

	public DriverHubTelemetry(Orchestrator orchestrator) {
		super(orchestrator);
		this.topicDataList = new PriorityBlockingQueue<>(1000, Comparator.comparingInt(DriverHubTelemetry::topicDataImportanceScore));
		this.topicData = new ConcurrentHashMap<>();
		this.opModeManager = OpModeManagerImpl.getOpModeManagerOfActivity(AppUtil.getInstance().getActivity());
		orchestrator.subscribeToPattern(
			Pattern.compile("^(?!log/).*"),
			(datum, topicName) -> {
				topicData.computeIfAbsent(topicName, tn -> {
					var result = new TopicData(tn, datum);
					topicDataList.add(result);
					return result;
				}).data = datum;
			}
		);

		addLogSubscription("err", "red");
		addLogSubscription("warn", "yellow");
		addLogSubscription("info", "green");
	}

	private void addLogSubscription(String logType, String colour) {
		var openingTag = String.format("<p style='color:%s; margin-bottom:0;'>", colour);
		var closingTag = "</p>";

		orchestrator.subscribe(
			"log/" + logType,
			String.class,
			text -> {
				var displayText = openingTag + text + closingTag;
				var front = logs.peekFirst();
				if (front == null || !front.equals(displayText))
					logs.addFirst(displayText);
			}
		);
	}

	@Override
	public void close() {
		super.close();
	}

	@RunPeriodically(maxFrequency = 1)
	void setMode() {
		NetworkConnectionHandler.getInstance().sendCommand(
			new Command(RobotCoreCommandList.CMD_SET_TELEMETRY_DISPLAY_FORMAT, "HTML"));
	}

	@RunPeriodically(maxFrequency = 10)
	public void updateTelemetry() {
		if (opModeManager.getActiveOpModeName().equals(OpModeManager.DEFAULT_OP_MODE_NAME))
			return;

		var message = new TelemetryMessage();
		for (var line : TelemetryTree.format(topicDataList).split("\n")) {
			message.addData(line, "");
		}
		message.setSorted(false);

		while (logs.size() > MAX_LOG_ENTRIES)
			logs.removeLast();

		logs.forEach(n -> message.addData("", n));
		opModeManager.refreshUserTelemetry(message, 0);
	}

	@SubscribedTo(topic = "telemetry/clear")
	public void clear() {
		topicDataList.clear();
		topicData.clear();
		logs.clear();
		opModeManager.refreshUserTelemetry(new TelemetryMessage(), 0);
	}

	private static int topicDataImportanceScore(TopicData data) {
		if (data.topicName.contains("err/")) return 5;
		if (data.topicName.contains("telemetry/")) return 4;
		if (data.topicName.contains("warn/")) return 3;
		if (data.topicName.contains("log/")) return 2;
		return 1;
	}
}
