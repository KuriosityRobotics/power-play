package com.kuriosityrobotics.powerplay.io;

import com.kuriosityrobotics.powerplay.hardware.ServoControl;
import com.qualcomm.robotcore.hardware.Servo;

import static java.lang.Math.PI;
import static java.lang.Math.toRadians;

class Claw extends ServoControl {
	// https://www.promodeler.com/DS930BLHV
	private static final double CLAW_SERVO_SPEED_RADS = toRadians(60) / .12;
	private static final double CLAW_SERVO_RANGE_RAD = toRadians(150);


	public Claw(Servo servo) {
		super(servo, CLAW_SERVO_SPEED_RADS, CLAW_SERVO_RANGE_RAD);
	}

	public void goToClawPosition(ClawPosition position) throws InterruptedException {
		goToAngle(position.angle);
		if (position == ClawPosition.SHUT)
			goToAngle(position.angle + toRadians(3)); // relax the servo
	}

	enum ClawPosition {
		OPEN(0),
		SHUT(toRadians(135));

		final double angle;

		ClawPosition(double angle) {
			this.angle = angle;
		}
	}
}
