package com.kuriosityrobotics.powerplay;

import com.kuriosityrobotics.powerplay.debug.StdoutTopicLogger;
import com.kuriosityrobotics.powerplay.localisation.kf.MotionModelLocaliser;
import com.kuriosityrobotics.powerplay.localisation.odometry.Odometry;
import com.kuriosityrobotics.powerplay.physics.NonPredictingDrive;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.teleop.R1TeleopController;

public class Simulator {
   public static void main(String[] args) {
	  try (var orch = Orchestrator.create("Simulator")) {
		  orch.startBridge();
		 orch.startNode("logger", new StdoutTopicLogger(orch));

		 orch.startNode("odometry", new Odometry(orch));
		 orch.startNode("localiser", new MotionModelLocaliser(orch, new NonPredictingDrive()));

		 orch.startNode("teleopController", new R1TeleopController(orch));

		 orch.publisher("a", Object.class).publish(new Object());
//		 Path path = new Path(new Pose[]{new Pose(0, 0, 0), new Pose(5, 0, 0), new Pose(5, 5, Math.PI/2), new Pose(2, 7, Math.PI/2)});
//		 double followRadius = 1;
//
//		 PurePursuit straightLine = new PurePursuit(orch, followRadius);
//		 orch.startNode("st", straightLine);
//		 orch.dispatch("path", path);

		  while (!Thread.interrupted()) Thread.yield();
	  }
   }
}
