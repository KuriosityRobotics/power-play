package com.kuriosityrobotics.powerplay.opmodes;

import com.kuriosityrobotics.powerplay.drive.DrivetrainController;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.kuriosityrobotics.powerplay.teleop.R1TeleopController;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

@com.qualcomm.robotcore.eventloop.opmode.TeleOp
@Disabled
public class DriveOnly extends OrchestratedOpMode {
	@Override
	protected void initOpMode(HardwareOrchestrator orchestrator) {
		super.initOpMode(orchestrator);
		orchestrator.stopAllNodes();

		orchestrator.startNode("drivetrainController", new DrivetrainController(orchestrator));
		orchestrator.startNode("teleopController", new R1TeleopController(orchestrator));
	}
}
