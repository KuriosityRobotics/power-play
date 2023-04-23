package com.kuriosityrobotics.powerplay.opmodes;

import static java.lang.Math.toRadians;

import android.util.Pair;

import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp
@Disabled
public class OuttakeCalibrate extends OrchestratedOpMode {
	public static double OUTTAKE_AUTO_START_HEIGHT = .5, OUTTAKE_AUTO_START_ANGLE = toRadians(40);

	@Override
	protected void initOpMode(HardwareOrchestrator orchestrator) {
		orchestrator.dispatch("outtake/calibrate", Pair.create(OUTTAKE_AUTO_START_HEIGHT, OUTTAKE_AUTO_START_ANGLE));
		orchestrator.dispatch("intake/claw/open", "");
	}
}
