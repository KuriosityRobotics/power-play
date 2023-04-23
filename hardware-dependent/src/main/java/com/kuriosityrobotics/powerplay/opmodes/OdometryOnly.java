package com.kuriosityrobotics.powerplay.opmodes;

import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.debug.DriverHubTelemetry;
import com.kuriosityrobotics.powerplay.drive.DrivetrainController;
import com.kuriosityrobotics.powerplay.localisation.IMUNode;
import com.kuriosityrobotics.powerplay.localisation.odometry.OdometryIntegrator;
import com.kuriosityrobotics.powerplay.localisation.odometry.Odometry;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.kuriosityrobotics.powerplay.teleop.R2TeleopController;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

@com.qualcomm.robotcore.eventloop.opmode.TeleOp
@Disabled
public class OdometryOnly extends OrchestratedOpMode {
	@Override
	protected void initOpMode(HardwareOrchestrator orchestrator) {
		super.initOpMode(orchestrator);
		orchestrator.stopAllNodes();
		orchestrator.startNode("telemetry", new DriverHubTelemetry(orchestrator));

		orchestrator.startNode("IMU", new IMUNode(orchestrator));
		orchestrator.startNode("odometry", new Odometry(orchestrator));
		orchestrator.startNode("localiser", new OdometryIntegrator(orchestrator, new Pose(0, 0, 0)));

		orchestrator.startNode("drivetrainController", new DrivetrainController(orchestrator));
		orchestrator.startNode("teleopController", new R2TeleopController(orchestrator));
	}

	@Override
	protected void cleanupOpMode(HardwareOrchestrator orchestrator) {
		orchestrator.stopAllNodes();
	}
}
