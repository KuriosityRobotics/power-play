package com.kuriosityrobotics.powerplay.drive;

import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.CONTROL_HUB;
import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.EXPANSION_HUB;
import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD;
import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE;

import com.kuriosityrobotics.powerplay.pubsub.MotorOrEncoder;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

/**
 * This class subscribes to the motorPowers topic and sets the physical motor voltages accordingly
 */
public class DrivetrainController extends Node {
	@MotorOrEncoder(hub = EXPANSION_HUB, port = 2, direction = FORWARD)
	private DcMotorEx backLeft;
	@MotorOrEncoder(hub = EXPANSION_HUB, port = 1, direction = REVERSE)
	private DcMotorEx frontRight;
	@MotorOrEncoder(hub = EXPANSION_HUB, port = 3, direction = FORWARD)
	private DcMotorEx frontLeft;
	@MotorOrEncoder(hub = EXPANSION_HUB, port = 0, direction = REVERSE)
	private DcMotorEx backRight;

	public DrivetrainController(Orchestrator orchestrator) {
		super(orchestrator);
	}

	@SubscribedTo(topic = "motorPowers")
	public void setMotorVoltages(MotorPowers voltages) {
		if (backLeft.isOverCurrent() || frontRight.isOverCurrent() || frontLeft.isOverCurrent() || backRight.isOverCurrent()) {
			voltages = MotorPowers.ofPowers(0, 0, 0, 0);
		}

		frontLeft.setPower(voltages.powerFrontLeft());
		backLeft.setPower(voltages.powerBackLeft());
		frontRight.setPower(voltages.powerFrontRight());
		backRight.setPower(voltages.powerBackRight());
	}

	@SubscribedTo(topic = "hardware/arm")
	void arm() {
		frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
		frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
		backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
		backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
	}
}
