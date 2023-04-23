package com.kuriosityrobotics.powerplay.localisation;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Servomotor;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.qualcomm.robotcore.hardware.Servo;

public class LiftingOdoNode extends Node {
	private static final double LIFT_POSITION = 0.;
	private static final double DOWN_POSITION = 1.;

	@Servomotor(hub = RobotConstants.LynxHub.CONTROL_HUB, port = 0)
	public Servo odoLifter;

	public LiftingOdoNode(Orchestrator orchestrator) {
		super(orchestrator);
	}

	@SubscribedTo(topic = "odo/lift")
	public void liftOdo(){
		odoLifter.setPosition(LIFT_POSITION);
	}

	@SubscribedTo(topic = "odo/drop")
	public void dropOdo(){
		odoLifter.setPosition(DOWN_POSITION);
	}
}
