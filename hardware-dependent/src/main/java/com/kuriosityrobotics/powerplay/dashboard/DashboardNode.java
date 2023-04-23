package com.kuriosityrobotics.powerplay.dashboard;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.kuriosityrobotics.powerplay.navigation.Path;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;

import java.util.ArrayList;
import java.util.List;

public class DashboardNode extends Node {
	private final FtcDashboard dashboard;
	private TelemetryPacket currentpacket;
	private boolean poseDrawn = false;
	private boolean pathDrawn = false;
	private List<Point> poseHistory;

	/**
	 * Registers the node with the orchestrator using {@link
	 * Orchestrator#markNodeBeingConstructed(Node)}. This will NOT start calling the update
	 * function, and will give this node instance a placeholder name
	 *
	 * <p>This exists so telemetry works inside the constructor
	 *
	 * @param orchestrator the orchestrator on which to start the node
	 */
	public DashboardNode(Orchestrator orchestrator) {
		super(orchestrator);

		dashboard = FtcDashboard.getInstance();
		currentpacket = new TelemetryPacket();
		poseDrawn = false;
		pathDrawn = false;
		poseHistory = new ArrayList<>(500);
	}

	@RunPeriodically(maxFrequency = 10)
	public void generateNewPacket() {
		dashboard.sendTelemetryPacket(currentpacket);

		currentpacket = new TelemetryPacket();
		poseDrawn = false;
		pathDrawn = false;
	}

	@SubscribedTo(topic = "localisation")
	public void updatePosition(LocalisationDatum localisationDatum) {
		Pose tmp = localisationDatum.pose().toFTCSystem();
		poseHistory.add(tmp);
		if (!poseDrawn) DashboardUtil.drawRobot(currentpacket.fieldOverlay(), tmp);

		poseDrawn = true;
	}

	@SubscribedTo(topic = "path")
	public void updatePath(Path path) {
		if (!pathDrawn) DashboardUtil.drawPath(currentpacket.fieldOverlay(), path.toFTCSystem());

		pathDrawn = true;
	}

	@RunPeriodically(maxFrequency = 1)
	public void drawPoseHistory() {
		DashboardUtil.drawPoseHistory(currentpacket.fieldOverlay(), poseHistory);
	}
}