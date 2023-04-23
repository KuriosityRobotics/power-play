package com.kuriosityrobotics.powerplay.autonomous;

import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.auto.AutoUtils;
import com.kuriosityrobotics.powerplay.io.DepositTarget;
import com.kuriosityrobotics.powerplay.io.PickupTarget;
import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.mpc.MPCNode;
import com.kuriosityrobotics.powerplay.navigation.Path;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Disabled
@Autonomous(group = "drive")
public class RedTriangleFunny extends OrchestratedOpMode {
	public void initOpMode(HardwareOrchestrator or) {
		or.stopAllNodes();
		or.startDefaultNodes();
	}

	public void runOpMode(HardwareOrchestrator or) throws InterruptedException, ExecutionException {
		var utils = new AutoUtils(or);
		Pose startPoint = new Pose(0.17, 2.6576, toRadians(90));

		Point cyclePoint = new Point(1.7, 2.7426);

//		GENERIC PATH SEQUENCE

		or.dispatchSynchronous("mpc/maintainConstantHeading", true);
		or.dispatchSynchronous("localisation/reset-position", startPoint);

		or.dispatchSynchronous("mpc/setTargetAngle", toRadians(90 - 14));
		var preload = new Point(1.255, 3.6576 - .95 - .03);
		or.dispatchSynchronous("mpc/resetPath", new Path(startPoint, preload));
		utils.awaitStablePosition();

		or.dispatchSynchronous("io/outtake/target", DepositTarget.MEDIUM_POLE);
		utils.depositPreload(true);
		or.startActionAsync("io/intake/farExtend");
		or.dispatchSynchronous("io/intake/target", PickupTarget.fromStackHeight(5));
		utils.awaitStablePosition();
		or.startActionAsync("io/intake/farPickup").get();
		or.startActionAsync("io/transfer").get();

		or.dispatchSynchronous("mpc/setTargetAngle", toRadians(90 + 14));
		var cycle1 = preload.add(new Point(.435, 0));
		or.dispatchSynchronous("mpc/resetPath", new Path(preload, cycle1.add(new Point(-.1, .1)), cycle1));
		utils.awaitStablePosition();
		or.dispatchSynchronous("io/outtake/target", DepositTarget.HIGH_POLE);
		var deposit = or.startActionAsync("io/deposit");
		or.startActionAsync("io/intake/farExtend");
		deposit.get();
		or.startActionAsync("io/intake/farPickup").get();
		or.startActionAsync("io/transfer").get();

		or.dispatchSynchronous("mpc/setTargetAngle", toRadians(90 - 14));
		var cycle2 = Point.ofTileNumber(2, 3).add(new Point(-.33, -.08));
		or.dispatchSynchronous("mpc/resetPath", new Path(cycle1, cycle1.add(new Point(0, .05)), cycle1.add(new Point(-.05, .05)), cycle2));
		utils.awaitStablePosition();
		var m = or.startActionAsync("io/deposit");
		m.get();

		or.dispatchSynchronous("mpc/setTargetAngle", toRadians(90));
		var p3 = Point.ofTileNumber(2, 4);
		or.dispatchSynchronous("mpc/resetPath", new Path(cycle2, Point.ofTileNumber(2, 3), p3));
		utils.awaitStablePosition();
		or.startActionAsync("io/intake/farPickup").get();
		or.startActionAsync("io/transfer").get();

		or.dispatchSynchronous("io/outtake/target", DepositTarget.HIGH_POLE);
		or.dispatchSynchronous("mpc/setTargetAngle", toRadians(90 + 14));
		var p4 = Point.ofTileNumber(2, 2).add(new Point(-.33 + .53, -.12));
		or.dispatchSynchronous("mpc/resetPath", new Path(p3, p4));
		utils.awaitStablePosition();
		or.startActionAsync("io/deposit").get();

//		utils.depositPreload();
//		utils.doAutoCycles(5, 5);
	}

	@Override
	protected void cleanupOpMode(HardwareOrchestrator orchestrator) {
		orchestrator.stopAllNodes();
	}
}
