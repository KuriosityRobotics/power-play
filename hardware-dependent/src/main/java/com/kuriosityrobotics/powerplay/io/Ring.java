package com.kuriosityrobotics.powerplay.io;

import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.hardware.ServoControl;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoImplEx;

public class Ring extends ServoControl {
	private static final double SERVO_SPEED_RADS = toRadians(60) / .1;
	private static final double SERVO_RANGE_RAD = toRadians(130);

	public Ring(Servo servo) {
		super(servo, SERVO_SPEED_RADS, SERVO_RANGE_RAD);
	}

	public void goToRingPosition(RingPosition position) throws InterruptedException {
		goToAngle(position.position);
	}

	public enum RingPosition {
		TRANSFER(toRadians(130)),
		DEPOSIT(toRadians(2));

		final double position;

		RingPosition(double position) {
			this.position = position;
		}
	}
}
