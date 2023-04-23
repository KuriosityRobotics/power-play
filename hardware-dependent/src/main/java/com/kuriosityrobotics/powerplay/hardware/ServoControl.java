/**
 * The ServoControl class provides methods for controlling a servo motor's position and speed.
 */
package com.kuriosityrobotics.powerplay.hardware;

import androidx.annotation.NonNull;

import com.kuriosityrobotics.powerplay.util.Duration;
import com.kuriosityrobotics.powerplay.util.Instant;
import com.kuriosityrobotics.powerplay.util.PreemptibleLock;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.Optional;

import static com.kuriosityrobotics.powerplay.util.ConcurrencyUtils.sleep;
import static java.lang.Math.*;

public class ServoControl {
	private final PreemptibleLock lock = new PreemptibleLock();
	private final double servoSpeedRads;
	private final double rangeRad;

	protected final Servo servo;
	private final boolean flipDirection;
	private final double zeroPosition;

	public ServoControl(Servo servo, double servoSpeedRads, double rangeRad, boolean flipDirection, double zeroPosition) {
		this.servo = servo;
		this.servoSpeedRads = servoSpeedRads;
		this.rangeRad = rangeRad;
		this.flipDirection = flipDirection;
		this.zeroPosition = zeroPosition;
	}

	public ServoControl(Servo servo, double servoSpeedRads, double rangeRad) {
		this(servo, servoSpeedRads, rangeRad, false, 0);
	}

	private Double previousServoPosition = null; // the last known servo position
	private Double currentServoTargetPosition = null; // where the servo is currently told to go to
	private Instant movementStartTime = null; // when the target Servo position was set

	/**
	 * This function returns the current position of a servo motor, based on its previous position, target
	 * position, and movement speed.
	 *
	 * @return The method returns an Optional object that may contain a Double value representing the
	 * current position of a servo motor. If the previous position is null, an empty Optional is returned.
	 * If there is no target position set, the method assumes that the servo is not moving and returns the
	 * previous position. Otherwise, it estimates the time required to reach the target position and
	 * calculates the current position based on the elapsed time,
	 */
	public Optional<Double> getServoPosition() {
		if (previousServoPosition == null || currentServoTargetPosition == null)
			return Optional.empty();

		var now = Instant.now();

		Duration movementDuration;
		if (this.currentServoTargetPosition == null)
			movementDuration = Duration.ofSeconds(0); // no target set, but previous position not null.  assume we are not moving rn
		else
			movementDuration = estimateTimeToPosition(currentServoTargetPosition);

		var movementEndTime = movementStartTime.add(movementDuration);
		if (now.isAfter(movementEndTime))
			return Optional.of((double) currentServoTargetPosition);
		else
			return Optional.of(previousServoPosition + now.since(movementStartTime).toSeconds() * servoSpeedRads * signum(currentServoTargetPosition - previousServoPosition));
	}

	/**
	 * `estimateTimeToPosition` estimates the time required to reach a target position, given the current
	 * position and the servo's speed. It assumes that the servo is moving at a constant speed.
	 *
	 * @param targetServoPosition The target position of the servo motor
	 * @return The method returns a Duration object representing the time required to reach the target position.
	 */
	@NonNull
	private Duration estimateTimeToPosition(double targetServoPosition) {
		double previousPosition;
		if (previousServoPosition == null) {
			double leftSideDistance = abs(targetServoPosition - 0);
			double rightSideDistance = abs(targetServoPosition - 2 * PI);

			previousPosition = leftSideDistance > rightSideDistance ? 0 : 2 * PI;
		} else {
			previousPosition = previousServoPosition;
		}

		return Duration.ofSeconds(abs(targetServoPosition - previousPosition) / servoSpeedRads);
	}

	/**
	 * This function sets the previous position of a servo to either its current position or a
	 * conservative estimate based on the target position.
	 *
	 * @param targetPosition The desired position that the servo should move to.
	 */
	private void conservativelySetPreviousServoPositionToPrevious(double targetPosition) {
		var currentPosition = getServoPosition();
		this.previousServoPosition = currentPosition.orElseGet(() -> { // we don't know where the Servo is;  be conservative
			// there are two worst cases:  the current Servo is at the start of its range, and the current Servo at the end.
			// Find the longest-running of the two, given the target position
			double leftSideDistance = abs(targetPosition - 0);
			double rightSideDistance = abs(targetPosition - 2 * PI);

			return leftSideDistance > rightSideDistance ? 0 : 2 * PI;
		});
	}

	/**
	 * This function sets the target position of a servo motor in radians and throws an exception if the
	 * position is out of range.
	 *
	 * @param targetPositionRad The target position of the servo in radians.
	 */
	private void setTargetServoPosition0(double targetPositionRad) {
		conservativelySetPreviousServoPositionToPrevious(targetPositionRad);


		double rawTargetPosition;
		if (flipDirection)
			rawTargetPosition = (zeroPosition - targetPositionRad) / rangeRad;
		else
			rawTargetPosition = (zeroPosition + targetPositionRad) / rangeRad;

		if (rawTargetPosition < 0 - 1e-6 || rawTargetPosition > 1 + 1e-6)
			throw new IllegalArgumentException("Servo position out of range: " + rawTargetPosition);

		this.currentServoTargetPosition = targetPositionRad;
		this.movementStartTime = Instant.now();
		servo.setPosition(rawTargetPosition);
	}

	/**
	 * This Java function sets the target servo position and sleeps for an estimated time to reach that
	 * position while holding a lock.
	 *
	 * @param position The desired angle position that the servo motor should move to.
	 */
	public void goToAngle(double position) throws InterruptedException {
		try {
			lock.lockForcibly();
			setTargetServoPosition0(position);
			sleep(estimateTimeToPosition(position));
		} finally {
			lock.unlock();
		}
	}

	public double minimumAngle() {
		return flipDirection ? zeroPosition - rangeRad : -zeroPosition;
	}

	public double maximumAngle() {
		return flipDirection ? zeroPosition : rangeRad - zeroPosition;
	}

	public boolean isInBounds(double angle) {
		return angle >= minimumAngle() && angle <= maximumAngle();
	}
}
