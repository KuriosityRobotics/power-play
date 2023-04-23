package com.kuriosityrobotics.powerplay.io;

import com.kuriosityrobotics.powerplay.hardware.ServoControl;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.toRadians;

class IntakeArm {
	private static final double ARM_SERVO_SPEED_REVS = toRadians(60) / .15;
	private static final double ARM_SERVO_RANGE_RAD = toRadians(165);

	private static final double ARM_LENGTH = .29;
	private static final double ARM_HEIGHT_OFFSET = .075;

	private final Orchestrator orchestrator;

	private final ServoControl leftServo;
	private final ServoControl rightServo;


	public IntakeArm(Orchestrator orchestrator, Servo leftServo, Servo rightServo) {
		this.orchestrator = orchestrator;
		this.leftServo = new ServoControl(leftServo, ARM_SERVO_SPEED_REVS, ARM_SERVO_RANGE_RAD / 2, false, 0);
		this.rightServo = new ServoControl(rightServo, ARM_SERVO_SPEED_REVS, ARM_SERVO_RANGE_RAD / 2, true, 0);
	}

	/**
	 * Moves the arm such that the middle of the claw is at the given height
	 *
	 * @param height the height to move to in meters
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	private void goToHeight(double height) throws InterruptedException, ExecutionException {
		goToAngle(calculateArmAngle(height));
	}

	/**
	 * Calculates the height of the cone contact point given the angle of the arm
	 *
	 * @param angle the angle of the arm in radians
	 * @return the height of the cone contact point in meters
	 */
	private static double calculateConeContactHeight(double angle) {
		return ARM_LENGTH * Math.sin(angle) + ARM_HEIGHT_OFFSET;
	}

	/**
	 * Calculates the angle of the arm given the height of the cone contact point
	 *
	 * @param height the height of the cone contact point in meters
	 * @return the angle of the arm in radians
	 */
	private static double calculateArmAngle(double height) {
		return Math.asin((height - ARM_HEIGHT_OFFSET) / ARM_LENGTH);
	}

	/**
	 * Gets the current position of the arm in radians
	 *
	 * @return the current position of the arm in radians
	 */
	public Optional<Double> getServoPosition() {
		if (leftServo.getServoPosition().isPresent() && rightServo.getServoPosition().isPresent()) {
			return Optional.of((leftServo.getServoPosition().get() + rightServo.getServoPosition().get()) / 2 * ARM_SERVO_RANGE_RAD);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Gets the current height of the cone contact point in meters
	 *
	 * @return the current height of the cone contact point in meters
	 */
	public Optional<Double> coneContactHeight() {
		return getServoPosition().map(IntakeArm::calculateConeContactHeight);
	}

	/**
	 * Gets the current angle of the arm in radians
	 *
	 * @return the current angle of the arm in radians
	 */
	public Optional<Double> armAngle() {
		return getServoPosition();
	}

	/**
	 * Moves the arm to the given pickup target
	 *
	 * @param target the target to move to
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	public void toPickup(PickupTarget target) throws InterruptedException, ExecutionException {
		goToHeight(target.getPickupHeight());
	}

	/**
	 * Moves the arm to the given hover target
	 *
	 * @param target the target to move to
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	public void toHover(PickupTarget target) throws InterruptedException, ExecutionException {
		goToHeight(target.getHoverHeight());
	}

	/**
	 * Moves the arm to the given angle
	 *
	 * @param angle the angle to move to in radians
	 * @throws InterruptedException if the thread is interrupted
	 */
	private void goToAngle(double angle) throws InterruptedException, ExecutionException {
		angle -= toRadians(46.5);

		// 1.  start by splitting the angle in half, so each of them contribute half
		var leftAngle = angle / 2;
		var rightAngle = angle / 2;

		// 2.  If they're both in bounds, great.  Otherwise, modify them
		if (!leftServo.isInBounds(leftAngle)) {
			double clippingLimit = (angle < leftServo.minimumAngle() ? leftServo.minimumAngle() : leftServo.maximumAngle());
			rightAngle += leftAngle - clippingLimit;
			leftAngle = clippingLimit;
		} else if (!rightServo.isInBounds(rightAngle)) {
			double clippingLimit = (angle < rightServo.minimumAngle() ? rightServo.minimumAngle() : rightServo.maximumAngle());
			leftAngle += rightAngle - clippingLimit;
			rightAngle = clippingLimit;
		}

		double finalLeftAngle = leftAngle;
		var leftServoMovement = orchestrator.startActionAsync(() -> leftServo.goToAngle(finalLeftAngle));
		double finalRightAngle = rightAngle;
		var rightServoMovement = orchestrator.startActionAsync(() -> rightServo.goToAngle(finalRightAngle));

		leftServoMovement.get();
		rightServoMovement.get();
	}

	/**
	 * Moves the arm to the neutral position (90 degrees)
	 *
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	public void toUpright() throws InterruptedException, ExecutionException {
		goToAngle(toRadians(90));
	}

	public void toNeutral() throws InterruptedException, ExecutionException {
		goToAngle(toRadians(100));
	}

	public void toFlat() throws InterruptedException, ExecutionException {
		goToAngle(toRadians(0));
	}

	/**
	 * Moves the arm to the ground junction deposit position
	 *
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	public void toGroundJunctionDeposit() throws InterruptedException, ExecutionException {
		goToAngle(toRadians(2));
	}

	/**
	 * Moves the arm to the transfer position (148.5 degrees)
	 *
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	public void toTransfer() throws InterruptedException, ExecutionException {
		goToAngle(toRadians(125));
	}

	/**
	 * Moves the arm to the lower untip position
	 * This is slightly below zero degrees
	 *
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	public void alignForUntip() throws InterruptedException, ExecutionException {
		goToAngle(toRadians(-5));
	}

	/**
	 * Moves the arm to the upper untip position
	 * This is slightly above zero degrees
	 *
	 * @throws InterruptedException if the thread is interrupted
	 * @throws ExecutionException   if the action fails
	 */
	public void attemptUntip() throws InterruptedException, ExecutionException {
		goToAngle(toRadians(45));
	}
}
