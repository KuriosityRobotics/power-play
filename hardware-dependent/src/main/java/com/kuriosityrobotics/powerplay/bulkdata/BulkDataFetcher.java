package com.kuriosityrobotics.powerplay.bulkdata;

import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.CONTROL_HUB;
import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.EXPANSION_HUB;
import static java.lang.Math.PI;

import com.kuriosityrobotics.powerplay.hardware.MotorFaults;
import com.kuriosityrobotics.powerplay.hardware.RobotConstants;
import com.kuriosityrobotics.powerplay.pubsub.Hub;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.qualcomm.hardware.lynx.LynxModule;

import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit;

public class BulkDataFetcher extends Node {
	private static final double COUNTS_PER_REVOLUTION = 8192.;

	@Hub(CONTROL_HUB)
	private LynxModule controlHub;
	@Hub(EXPANSION_HUB)
	private LynxModule expansionHub;

	@LastMessagePublished(topic = "motorFaults")
	private MotorFaults motorFaults = new MotorFaults();

	public BulkDataFetcher(Orchestrator orchestrator) {
		super(orchestrator);
	}

	@RunPeriodically(maxFrequency = 50)
	public void updateData() {
		orchestrator.dispatch("batteryVoltage", controlHub.getInputVoltage(VoltageUnit.VOLTS));
		dispatchBulkData("controlHub", RobotConstants.LynxHub.CONTROL_HUB, controlHub.getBulkData());
		dispatchBulkData("expansionHub", EXPANSION_HUB, expansionHub.getBulkData());

		orchestrator.dispatch("bulkData", "");
	}

	private int correctOverflow(int input){
		if (input%20 == 0) {
			return input;
		} else if ((input + (1 << 16))%20 == 0) {
			return input + (1 << 16);
		} else if ((input - (1 << 16))%20 == 0) {
			return input - (1 << 16);
		} else {
			err("Could not figure out how to prevent overflow and cast vel to multiple of 20");
			return input; // this edge case is not possible when using a throughbore encoder, where the reported velocity is always a multiple of 20 in some form
		}
	}

	/**
	 * Converts the BulkData to a {@link RevHubBulkData}.
	 *
	 * @param data The bulk data to convert.
	 * @return The converted bulk data.
	 */
	private void dispatchBulkData(String hubName, RobotConstants.LynxHub hub, LynxModule.BulkData data) {
		var result = new RevHubBulkData(hub);

		for (int i = 0; i < result.encoders.length; i++) {
			var position = data.getMotorCurrentPosition(i);
			result.encoders[i] = position;
			orchestrator.dispatch(hubName + "/encoder/" + i + "/position", 2 * PI * position / COUNTS_PER_REVOLUTION);
		}


		for (int i = 0; i < result.velocities.length; i++) {
			var correctedVelocity = correctOverflow(data.getMotorVelocity(i));
			result.velocities[i] = correctedVelocity;
			orchestrator.dispatch(hubName + "/encoder/" + i + "/velocity", 2 * PI * correctedVelocity / COUNTS_PER_REVOLUTION);
		}

		for (int i = 0; i < result.analogInputs.length; i++) {
			var voltage = data.getAnalogInputVoltage(i);
			result.analogInputs[i] = voltage;
			orchestrator.dispatch(hubName + "/analog/" + i, voltage);
		}

		for (int i = 0; i < result.digitalInputs.length; i++) {
			var state = data.getDigitalChannelState(i);
			result.digitalInputs[i] = state;
			orchestrator.dispatch(hubName + "/digital/" + i, state);
		}

		for (int i = 0; i < result.motorOverCurrentWarnings.length; i++) {
			var warning = motorFaults.forHub(hub)[i];
			result.motorOverCurrentWarnings[i] = warning;
			orchestrator.dispatch(hubName + "/currentWarnings/" + i, warning);
		}

		orchestrator.dispatch(hubName, result);
	}
}
