package com.kuriosityrobotics.powerplay.autonomous;

import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.auto.AutoUtils;
import com.kuriosityrobotics.powerplay.cameras.LeftCamera;
import com.kuriosityrobotics.powerplay.cameras.RightCamera;
import com.kuriosityrobotics.powerplay.cameras.VisionTask;
import com.kuriosityrobotics.powerplay.io.DepositTarget;
import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.mpc.MPCNode;
import com.kuriosityrobotics.powerplay.navigation.Path;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import java.util.concurrent.ExecutionException;

//@Disabled
@Autonomous(group = "drive")
public class RedTriangleAutomp extends OrchestratedOpMode {
	private final double FIELD_EDGE = 3.6448999;
	private int spot = 1;

	@Override
	protected void initOpMode(HardwareOrchestrator or) {
		super.initOpMode(or);
		or.stopAllNodes();
		or.startDefaultNodes();

		or.startNode("cam", new RightCamera(or));
		or.startNode("visi", new VisionTask(or));

		or.subscribe("vision/parkingspot", Integer.class, spot -> this.spot = spot);
		try {
			or.startNode("mpc", new MPCNode(or));
		} catch (InterruptedException e) {

		}
	}

	public void runOpMode(HardwareOrchestrator or) throws InterruptedException, ExecutionException {
		var utils = new AutoUtils(or);

		Pose startPoint = new Pose(FIELD_EDGE - 0.17, 1.0, toRadians(-90));
		Point preloadPoint = new Point(FIELD_EDGE - 1.73, 0.92);

		var startToPreload = new Path(startPoint, preloadPoint);

		or.dispatchSynchronous("mpc/maintainConstantHeading", false);
		or.dispatchSynchronous("localisation/reset-position", startPoint);
		or.dispatchSynchronous("mpc/setTargetAngle", toRadians(-90 + 14));
		or.dispatchSynchronous("mpc/motorWeight", .05);
		or.dispatchSynchronous("mpc/resetPath", startToPreload);

		utils.awaitStablePosition();
		or.dispatchSynchronous("mpc/motorWeight", .05);

		utils.depositPreload(false);

		var cyclePoint = preloadPoint.add(new Point(.49, .03));
		var preloadToCycle = new Path(preloadPoint, cyclePoint);
		or.dispatchSynchronous("mpc/setTargetAngle", toRadians(-90 - 14));
//		or.dispatchSynchronous("mpc/motorWeight", .05);
		or.dispatchSynchronous("mpc/resetPath", preloadToCycle);
		utils.awaitStablePosition();

		utils.setDepositTarget(DepositTarget.MEDIUM_POLE);
		utils.doAutoCycles(5, 5, false);

		var cycleToP1 = new Path(cyclePoint, Point.ofTileNumber(5 - 1, 1), Point.ofTileNumber(5 - 1, 2).add(new Point(.04, 0))); // subtrsct 2cm so the intake doesnt overhang
		var cycleToP2 = new Path(cyclePoint, Point.ofTileNumber(5 - 1, 1).add(new Point(.04, 0)));
		var cycleToP3 = new Path(cyclePoint, Point.ofTileNumber(5 - 1, 1), Point.ofTileNumber(5 - 1, 0).add(new Point(.04, 0)));

		or.dispatchSynchronous("mpc/maintainConstantHeading", true);
		if (this.spot == 1) {
			or.dispatchSynchronous("mpc/setTargetAngle", toRadians(0));
			or.dispatchSynchronous("mpc/resetPath", cycleToP1);
		} else if (spot == 2) {
			or.dispatchSynchronous("mpc/setTargetAngle", toRadians(0));
			or.dispatchSynchronous("mpc/resetPath", cycleToP2);
		}else if (spot == 3) {
			or.dispatchSynchronous("mpc/setTargetAngle", toRadians(180));
			or.dispatchSynchronous("mpc/resetPath", cycleToP3);
		}


		utils.awaitStablePosition();
	}

	@Override
	protected void cleanupOpMode(HardwareOrchestrator orchestrator) {
		orchestrator.stopNode("mpc");
		orchestrator.stopNode("cam");
		orchestrator.stopNode("visi");
	}
}
