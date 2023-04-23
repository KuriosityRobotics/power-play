package com.kuriosityrobotics.powerplay.opmodes;

import com.kuriosityrobotics.powerplay.outtake.VeloOuttakeNode;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

import java.util.concurrent.ExecutionException;

@com.qualcomm.robotcore.eventloop.opmode.TeleOp
@Disabled
public class funny extends OrchestratedOpMode {
	@Override
	protected void initOpMode(HardwareOrchestrator orchestrator) {
		super.initOpMode(orchestrator);
		orchestrator.stopAllNodes();

		orchestrator.startNode("outtake", new VeloOuttakeNode(orchestrator));
	}

	@Override
	protected void runOpMode(HardwareOrchestrator orchestrator) throws InterruptedException, ExecutionException {
		super.runOpMode(orchestrator);
		orchestrator.dispatch("telemetry/clear", "");
	}
}
