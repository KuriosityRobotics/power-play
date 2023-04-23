package com.kuriosityrobotics.powerplay.mpc;

import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.navigation.Path;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.io.IOException;

@TeleOp(name = "MPC Dynamic Test", group = "Test")
public class DynamicPathTestTeleop extends OrchestratedOpMode {

	@Override
	protected void initOpMode(HardwareOrchestrator or) {
		super.initOpMode(or);
		or.stopAllNodes();
		or.startDefaultNodes();
	}

	public void runOpMode(HardwareOrchestrator or) throws InterruptedException {

		Path path;
		try {
			path = Path.fromCSV(getClass().getClassLoader().getResourceAsStream("weave.csv"));
		} catch (IOException e) {
			or.err("Failed to load path: " + e.getMessage());
			return;
		}

		Path straightLinePath = new Path(new Point[] {new Point(0.1905, 1.016), new Point(1.1905, 1.016)});
		Path straightLineReturnPath1 = new Path(new Point[] {new Point(1.1905, 1.016), new Point(0.2805, 1.016)});
		Path straightLineReturnPath2 = new Path(new Point[] {new Point(1.2805, 1.016), new Point(0.2105, 1.016)});
		Path stationaryPath = new Path(new Point[] {new Point(0.1905, 1.016), new Point(0.1905, 1.016)});

//		or.dispatch("localisation/reset-position", new Pose(path.get(0), toRadians(0.)));
		or.dispatch("localisation/reset-position", new Pose(0.1905,1.016, toRadians(0)));
		Thread.sleep(10);


//		GENERIC PATH SEQUENCE

		or.dispatch("mpc/resetPath", path);
		or.dispatch("mpc/setTargetAngle", toRadians(0));
		or.dispatch("mpc/maintainConstantHeading", false);

//		FANCY PATH SEQUENCE

//		or.dispatch("mpc/resetPath", straightLinePath);
//		or.dispatch("mpc/maintainConstantHeading", false);
//		or.dispatch("mpc/setTargetAngle", toRadians(0));
//
//		Thread.sleep(3000);
//
//		or.dispatch("mpc/setTargetAngle", toRadians(45));
//
//		Thread.sleep(1000);
//
//		or.dispatch("mpc/resetPath", straightLineReturnPath1);
//		or.dispatch("mpc/maintainConstantHeading", true);
//
//		Thread.sleep(3000);
//
//		or.dispatch("mpc/setTargetAngle", toRadians(0));
//		or.dispatch("mpc/resetPath", straightLineReturnPath2);
//
//		Thread.sleep(3000);
//
//		or.dispatch("mpc/resetPath", path);
//		or.dispatch("mpc/setTargetAngle", toRadians(0));
//		or.dispatch("mpc/maintainConstantHeading", false);
	}

	@Override
	protected void cleanupOpMode(HardwareOrchestrator orchestrator) {
		orchestrator.stopAllNodes();
	}
}