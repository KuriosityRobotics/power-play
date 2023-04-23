package com.kuriosityrobotics.powerplay.io;

import static java.lang.Math.pow;
import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.hardware.HardwareException;
import com.kuriosityrobotics.powerplay.hardware.LinearMotorControl;
import com.kuriosityrobotics.powerplay.hardware.OverCurrentFault;
import com.kuriosityrobotics.powerplay.util.Duration;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

import java.util.concurrent.locks.ReentrantLock;

public class OuttakeExtensionSlides extends LinearMotorControl {
	private static final double TICKS_PER_METRE = 992.970132;
	private static final double SLIDES_ANGLE = toRadians(60);
	private static final double FLOOR_TO_RING_DISTANCE = .33;


	private final DcMotorEx leftMotor;
	private final DcMotorEx rightMotor;

	public OuttakeExtensionSlides(DcMotorEx leftMotor, DcMotorEx rightMotor) {
		super(Duration.ofSeconds(3));
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
	}

	@Override
	protected boolean isBusy() {
		//return leftMotor.isBusy() && rightMotor.isBusy(); // && instead of || because they might fight each other
		boolean leftMotorReady = Math.abs(leftMotor.getCurrentPosition() - leftMotor.getTargetPosition()) < 15;
		boolean rightMotorReady = Math.abs(rightMotor.getCurrentPosition() - rightMotor.getTargetPosition()) < 15;

		return !(leftMotorReady || rightMotorReady);
	}

	private int getCurrentPositionTicks() {
		return (leftMotor.getCurrentPosition() + rightMotor.getCurrentPosition()) / 2;
	}

	@Override
	protected double getCurrentPositionMetres() {
		return getCurrentPositionTicks() / TICKS_PER_METRE;
	}

	private void setTargetPositionTicks(int position) {
		leftMotor.setTargetPosition(position);
		rightMotor.setTargetPosition(position);
	}

	@Override
	protected void setTargetPositionMetres0(double position) {
		setTargetPositionTicks((int) (position * TICKS_PER_METRE));
	}

	@Override
	protected void throwIfOverCurrent() throws OverCurrentFault {
		if (isOverCurrent())
			throw new OverCurrentFault(String.format("Outtake slides over current. Left: %fA, Right: %fA", leftMotor.getCurrent(CurrentUnit.AMPS), rightMotor.getCurrent(CurrentUnit.AMPS)));
	}

	@Override
	protected boolean isOverCurrent() {
		return leftMotor.isOverCurrent() || rightMotor.isOverCurrent();
	}

	private boolean motorsDisabled = false;
	private double nominalLeftPower = 1, nominalRightPower = 1;

	public void goToPosition(OuttakeSlidePosition position) throws InterruptedException, HardwareException {
//		try {
//			super.goToPosition(position.position);
//		} catch (OverCurrentFault e) {
//			setMotorScaledPowers(0);
//			throw e;
//		}
//
//		setMotorPowersForTargetState(position);
		if ((leftMotor.getTargetPosition() == 0 || rightMotor.getTargetPosition() == 0) && position != OuttakeSlidePosition.RETRACTED) { //the last target is 0
			leftMotor.setPower(1);
			rightMotor.setPower(1);
		}else{
			leftMotor.setPower(0.1);
			rightMotor.setPower(0.1);
		}

		super.goToPosition(position.position);

		if (position == OuttakeSlidePosition.RETRACTED && (leftMotor.getPower() != 0 && rightMotor.getPower() != 0)) {
			leftMotor.setPower(0);
			rightMotor.setPower(0);
		}
	}

	private void setMotorPowersForTargetState(OuttakeSlidePosition position) throws InterruptedException {
		switch (position) {
			case RETRACTED:
			case LOW_POLE:
				if (isBusy())
					setMotorScaledPowers(0.1);
				else
					setMotorScaledPowers(0);
			case MEDIUM_POLE:
			case HIGH_POLE:
				setMotorScaledPowers(1);
		}
	}

	private final ReentrantLock enableDisableLock = new ReentrantLock();

	private void setMotorScaledPowers(double power) throws InterruptedException {
		enableDisableLock.lockInterruptibly();
		try {
			if (motorsDisabled)
				return;

			nominalLeftPower = leftMotor.getPower();
			nominalRightPower = rightMotor.getPower();

			leftMotor.setPower(power * nominalLeftPower);
			rightMotor.setPower(power * nominalRightPower);
			motorsDisabled = true;
		} finally {
			enableDisableLock.unlock();
		}
	}

	public void enableMotors() throws InterruptedException {
		enableDisableLock.lockInterruptibly();
		try {
			if (!motorsDisabled)
				return;

			leftMotor.setPower(nominalLeftPower);
			rightMotor.setPower(nominalRightPower);
			motorsDisabled = false;
		} finally {
			enableDisableLock.unlock();
		}
	}

	/**
	 * This enum represents the positions of the slides. The position is the distance the slides are extended, in metres.
	 */
	public enum OuttakeSlidePosition {
		RETRACTED(0),
		LOW_POLE(0),
		MEDIUM_POLE(calculateExtensionLength(.58)),
		HIGH_POLE(calculateExtensionLength(.86));

		/**
		 * The distance the slides are extended, in metres.
		 * This does not include the length of the slides when they are retracted.
		 */
		final double position;

		/**
		 * @param position the distance the slides are extended (diagonally), in metres
		 */
		OuttakeSlidePosition(double position) {
			this.position = position; // calculate the position we need to that the cone is placed exactlyat the top
		}

		/**
		 * Calculates the extension length of the slides, in metres, to place a cone at the specified height
		 */
		public static double calculateExtensionLength(double height) {
			return (height - FLOOR_TO_RING_DISTANCE) / Math.sin(SLIDES_ANGLE);
		}
	}
}
