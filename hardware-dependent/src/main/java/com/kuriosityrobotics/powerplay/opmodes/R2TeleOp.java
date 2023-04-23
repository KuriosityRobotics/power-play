package com.kuriosityrobotics.powerplay.opmodes;

import com.kuriosityrobotics.powerplay.localisation.LiftingOdoNode;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.kuriosityrobotics.powerplay.teleop.R2TeleopController;

@com.qualcomm.robotcore.eventloop.opmode.TeleOp
public class R2TeleOp extends OrchestratedOpMode {
	@Override
	protected void initOpMode(HardwareOrchestrator orchestrator) {
		//orchestrator.startDefaultNodes();
		orchestrator.startNode("TeleopController", new R2TeleopController(orchestrator));
		orchestrator.startNode("liftingodo", new LiftingOdoNode(orchestrator));
	}

	@Override
	protected void runOpMode(HardwareOrchestrator orchestrator) {
	}

	@Override
	protected void cleanupOpMode(HardwareOrchestrator orchestrator) {
		orchestrator.stopNode("TeleopController");
		orchestrator.stopNode("liftingodo");
	}

}
