package com.kuriosityrobotics.powerplay.io;

import static java.lang.Math.abs;

import com.kuriosityrobotics.powerplay.hardware.HardwareException;
import com.kuriosityrobotics.powerplay.hardware.LinearMotorControl;
import com.kuriosityrobotics.powerplay.hardware.OverCurrentFault;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.util.Duration;
import com.qualcomm.robotcore.hardware.ColorRangeSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.util.concurrent.locks.ReentrantLock;

class IntakeExtensionSlides extends LinearMotorControl {
	private static final double TICKS_PER_METRE = 611 / .55;
	private static final double HARDSTOP_ACTIVATION_DISTANCE = 84 / TICKS_PER_METRE;


	private final Orchestrator orchestrator;


	private final DcMotorEx rightExtension, leftExtension;
	private final ColorRangeSensor colorRangeSensor;



	IntakeExtensionSlides(Orchestrator orchestrator, DcMotorEx rightExtension, DcMotorEx leftExtension, ColorRangeSensor colorRangeSensor) {
		super(Duration.ofSeconds(3));
		this.orchestrator = orchestrator;
		this.rightExtension = rightExtension;
		this.leftExtension = leftExtension;
		this.colorRangeSensor = colorRangeSensor;
	}

	public boolean isHardstopEngaged() {
		return getCurrentPositionMetres() >= HARDSTOP_ACTIVATION_DISTANCE;
	}

	private final ReentrantLock enableDisableLock = new ReentrantLock();
	private double nominalPower;
	private boolean motorDisabled = false;

	private void disableMotors() throws InterruptedException {
		enableDisableLock.lockInterruptibly();
		try {
			if (motorDisabled)
				return;

			nominalPower = rightExtension.getPower();

			rightExtension.setPower(0);
			motorDisabled = true;
		} finally {
			enableDisableLock.unlock();
		}
	}

	public void enableMotors() throws InterruptedException {
		enableDisableLock.lockInterruptibly();
		try {
			if (!motorDisabled)
				return;

			rightExtension.setPower(nominalPower);
			motorDisabled = false;
		} finally {
			enableDisableLock.unlock();
		}
	}

	public void goToPosition(IntakeSlidePosition position) throws InterruptedException, HardwareException {
		super.goToPosition(position.position); // might return early - extend till we see a cone and then brake
		if (position == IntakeSlidePosition.EXTENDED_TELEOP) {
			brake();
		}
	}

	@Override
	protected boolean isBusy() {
		return colorRangeSensor.getDistance(DistanceUnit.METER) > .05 && rightExtension.isBusy();
	}

	@Override
	protected double getCurrentPositionMetres() {
		return rightExtension.getCurrentPosition() / TICKS_PER_METRE;
	}

	protected double getVelocityMetresPerSecond() {
		return rightExtension.getVelocity() / TICKS_PER_METRE;
	}

	@Override
	protected void setTargetPositionMetres0(double position) {
		rightExtension.setTargetPosition((int) (position * TICKS_PER_METRE));
	}

	public void brake() throws InterruptedException {
		lock.lockForcibly();
		try {
			rightExtension.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
			leftExtension.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

			rightExtension.setVelocity(0);
			leftExtension.setVelocity(0);

			leftExtension.setPower(rightExtension.getPower());
			while (isBusy() && !Thread.interrupted()) {
				Thread.sleep(30);
			}

			if (Thread.interrupted())
				throw new InterruptedException();
		} finally {
			leftExtension.setPower(0);
			setTargetPositionMetres0(getCurrentPositionMetres());

			leftExtension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
			rightExtension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
			lock.unlock();
		}
	}

	@Override
	protected void throwIfOverCurrent() throws OverCurrentFault {
		if (isOverCurrent())
			throw new OverCurrentFault(String.format("Intake slides over current. Current: %fA", rightExtension.getCurrent(CurrentUnit.AMPS)));
	}



	@Override
	protected boolean isOverCurrent() {
		return rightExtension.isOverCurrent();
	}

	public enum IntakeSlidePosition {
		GROUND_JUNCTION_DEPOSIT(HARDSTOP_ACTIVATION_DISTANCE),
		EXTENDED_TELEOP(.54),
		HARDSTOP(HARDSTOP_ACTIVATION_DISTANCE),
		FULLY_RETRACTED(0);

		final double position;

		IntakeSlidePosition(double position) {
			this.position = position;
		}
	}
}
