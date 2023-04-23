package com.kuriosityrobotics.powerplay.hardware;

import com.kuriosityrobotics.powerplay.bulkdata.RevHubBulkData;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.qualcomm.robotcore.hardware.DcMotorControllerEx;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

public class MotorCurrentWatchdog extends Node {
	private final DcMotorControllerEx controlHub, expansionHub;


	public MotorCurrentWatchdog(HardwareOrchestrator orchestrator) {
		super(orchestrator);
		controlHub = orchestrator.getHardwareProvider().motorControllerFor(RobotConstants.LynxHub.CONTROL_HUB);
		expansionHub = orchestrator.getHardwareProvider().motorControllerFor(RobotConstants.LynxHub.EXPANSION_HUB);
	}

	@LastMessagePublished(topic = "controlHub")
	private RevHubBulkData controlHubData;
	@LastMessagePublished(topic = "expansionHub")
	private RevHubBulkData expansionHubData;

	private final int RING_BUFFER_SIZE = 5;

	private final double[][] controlHubMotorCurrents = new double[RING_BUFFER_SIZE][4];
	private final double[][] expansionHubMotorCurrents = new double[RING_BUFFER_SIZE][4];
	private int idx = 0;

	private final double[][] controlHubCurrentDiff = new double[RING_BUFFER_SIZE][4];
	private final double[][] expansionHubCurrentDiff = new double[RING_BUFFER_SIZE][4];


	@RunPeriodically(maxFrequency = 10)
	private void checkCurrent() {
		readCurrents();

		// a FAULT occurs if both:
		// 1. the current is greater than the set alert limit
		// 2. the current has changed by more than 50% of the alert limit in the .2 seconds
		var motorFaults = new MotorFaults();

		for (int i = 0; i < controlHubMotorCurrents.length; i++) {
			var alertLimit = controlHub.getMotorCurrentAlert(i, CurrentUnit.AMPS);

			boolean alertLimitConditionMet = controlHubMotorCurrents[idx][i] > alertLimit;
			boolean derivativeConditionMet = false;
			for (double[] doubles : controlHubCurrentDiff)
				derivativeConditionMet |= doubles[i] > alertLimit * 0.5;

			motorFaults.controlHub[i] = alertLimitConditionMet && derivativeConditionMet;
		}

		for (int i = 0; i < expansionHubMotorCurrents.length; i++) {
			var alertLimit = expansionHub.getMotorCurrentAlert(i, CurrentUnit.AMPS);

			boolean alertLimitConditionMet = expansionHubMotorCurrents[idx][i] > alertLimit;
			boolean derivativeConditionMet = false;
			for (double[] doubles : expansionHubCurrentDiff)
				derivativeConditionMet |= doubles[i] > alertLimit * 0.5;

			motorFaults.expansionHub[i] = alertLimitConditionMet && derivativeConditionMet;
		}

		idx = (idx + 1) % RING_BUFFER_SIZE;
		orchestrator.dispatch("motorFaults", motorFaults);
	}

	private void readCurrents() {
		for (int i = 0; i < controlHubMotorCurrents.length; i++) {
			controlHubMotorCurrents[idx][i] = controlHub.getMotorCurrentPosition(i);
			controlHubCurrentDiff[idx][i] = controlHubMotorCurrents[idx][i] - controlHubMotorCurrents[(idx + RING_BUFFER_SIZE - 2) % RING_BUFFER_SIZE][i];
		}
		for (int i = 0; i < expansionHubMotorCurrents.length; i++) {
			expansionHubMotorCurrents[idx][i] = expansionHub.getMotorCurrentPosition(i);
			expansionHubCurrentDiff[idx][i] = expansionHubMotorCurrents[idx][i] - expansionHubMotorCurrents[(idx + RING_BUFFER_SIZE - 2) % RING_BUFFER_SIZE][i];
		}
	}
}
