package com.kuriosityrobotics.powerplay.localisation;

import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.pubsub.NamedHardware;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;

import com.kuriosityrobotics.powerplay.util.Instant;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.IMU;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import static java.lang.Math.PI;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import java.util.concurrent.locks.ReentrantLock;

public class IMUNode extends Node {
	public double startAngle;
	private double prevAngle;

	@NamedHardware("imu")
	private IMU imu;

	public IMUNode(Orchestrator orchestrator) {
		super(orchestrator);

		var logoDirection = RevHubOrientationOnRobot.LogoFacingDirection.RIGHT;
		var usbDirection = RevHubOrientationOnRobot.UsbFacingDirection.FORWARD;
		var orientationOnRobot = new RevHubOrientationOnRobot(logoDirection, usbDirection);

		imu.initialize(new com.qualcomm.robotcore.hardware.IMU.Parameters(orientationOnRobot));
		imu.resetYaw();
	}

	private double lastRawAngle = 0;
	private Instant lastAngleTime = Instant.now();
	private int fullRevolutionCount = 0;
	private final ReentrantLock angleLock = new ReentrantLock();

	public double getYaw() throws InterruptedException {
		double rawAngle = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);

		angleLock.lockInterruptibly();
		try {
			if (Instant.now().since(lastAngleTime).toSeconds() <= .5) { // if our last reading was more than half a second ago, it's plausible that we've actually rotated
				if (rawAngle - lastRawAngle > PI)
					fullRevolutionCount--;
				else if (rawAngle - lastRawAngle < -PI)
					fullRevolutionCount++;
			}

			lastRawAngle = rawAngle;
			lastAngleTime = Instant.now();
			return startAngle + rawAngle + fullRevolutionCount * 2 * PI;
		} finally {
			angleLock.unlock();
		}
	}

	public double getYawVelocity() {
		var result = toRadians(imu.getRobotAngularVelocity(AngleUnit.DEGREES).zRotationRate);
		return result;
	}

	/**
	 * Find the robot's angle using the imu and publish it to the angle topic
	 */
	@RunPeriodically(maxFrequency = 20)
	private void publishAngle() throws InterruptedException {
		orchestrator.dispatch("angle", getYaw());
		orchestrator.dispatch("angular_vel", getYawVelocity());
	}

	@SubscribedTo(topic = "localisation/reset-position")
	private void resetAngle(Pose newPos) throws InterruptedException {
		angleLock.lockInterruptibly();
		try {
			this.startAngle = newPos.orientation();
			this.lastRawAngle = 0;
			this.lastAngleTime = Instant.now();
			this.fullRevolutionCount = 0;
			imu.resetYaw();
		} finally {
			angleLock.unlock();
		}
	}
}
