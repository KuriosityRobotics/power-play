package com.kuriosityrobotics.powerplay.teleop;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.io.DepositTarget;
import com.kuriosityrobotics.powerplay.io.PickupTarget;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.navigation.Path;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;

/**
 * This class subscribes to various gamepad events and then publishes the appropriate messages to
 * the appropriate mechanism/drivetrain topics
 */
public class R2TeleopController extends Node {

	@LastMessagePublished(topic = "gamepad1/left_stick_x")
	private double left_stick_x;

	@LastMessagePublished(topic = "gamepad1/left_stick_y")
	private double left_stick_y;

	@LastMessagePublished(topic = "gamepad1/right_stick_x")
	private double right_stick_x;

	@LastMessagePublished(topic = "gamepad2/right_stick_y")
	private double change_height;
	private boolean change_height_flag = false;

	@LastMessagePublished(topic = "localisation")
	private LocalisationDatum localisationDatum;

	@LastMessagePublished(topic = "reverseDriving")
	private boolean reverseDriving = false;

	/**
	 * This class subscribes to various gamepad events and then publishes the appropriate messages
	 * to the appropriate mechanism/drivetrain topics
	 */
	public R2TeleopController(Orchestrator orchestrator) {
		super(orchestrator);
	}

	/**
	 * Converts the gamepad input into motor voltages
	 *
	 * @param xMov    the x-axis movement of the left joystick
	 * @param yMov    the y-axis movement of the left joystick
	 * @param turnMov the x-axis movement of the right joystick
	 * @return the motor voltages to send to the drivetrain
	 */
	private MotorPowers movementsToMotorVoltages(double xMov, double yMov, double turnMov) {
		return MotorPowers.ofPowers(
			(xMov + yMov - turnMov),
			(xMov - yMov + turnMov),
			(xMov - yMov - turnMov),
			(xMov + yMov + turnMov));
	}

	private static final double DEADZONE = .01;

	@SubscribedTo(topic = "gamepad1/left_stick_x")
	@SubscribedTo(topic = "gamepad1/left_stick_y")
	@SubscribedTo(topic = "gamepad1/right_stick_x")
	public void updateMotorVoltages() {
		orchestrator.dispatch(
			"motorPowers",
			movementsToMotorVoltages((reverseDriving ? -1 : 1) * Math.signum(left_stick_y) * Math.pow(abs(left_stick_y), 1.),
				(reverseDriving ? -1 : 1) * Math.signum(left_stick_x) * -Math.pow(abs(left_stick_x), 1.),
				-Math.signum(right_stick_x) * Math.pow(abs(right_stick_x), 1.))
		);
	}

	@SubscribedTo(topic = "gamepad2/right_stick_y")
	public void updateHeight() {
		boolean curr = abs(change_height) > 1e-6;

		if (curr && !change_height_flag) {
			if (change_height < 0) orchestrator.dispatch("intake/change_up", "");
			else orchestrator.dispatch("intake/change_down", "");
		}

		change_height_flag = curr;
	}

	@SubscribedTo(topic = "gamepad1/a/rising")
	public void reverseDriving() {
		reverseDriving = !reverseDriving;
	}

	@SubscribedTo(topic = "gamepad2/right_bumper/rising")
	public void down() {
		orchestrator.dispatchSynchronous("io/intake/target", PickupTarget.ONE_STACK);
		orchestrator.startActionAsync("io/intake/nearPickup");
	}

	@SubscribedTo(topic = "gamepad2/left_bumper/rising")
	void topConePickup() {
		orchestrator.dispatchSynchronous("io/intake/target", PickupTarget.FIVE_STACK);
	}

	private boolean leftTriggerPrevious = false;

	@SubscribedTo(topic = "gamepad2/left_trigger")
	public void midHeightPickup(double trigger) {
		boolean leftTriggerPressed = trigger > 0.2;
		if (leftTriggerPressed && !leftTriggerPrevious) {
			orchestrator.dispatchSynchronous("io/intake/target", PickupTarget.THREE_STACK);
		}
		leftTriggerPrevious = leftTriggerPressed;
	}

	@SubscribedTo(topic = "gamepad2/b/rising")
	public void extend() {
		orchestrator.dispatchSynchronous("io/intake/target", PickupTarget.ONE_STACK);
		orchestrator.startActionAsync("io/intake/farPickup");
	}

	private boolean rightTriggerPrevious = false;

	@SubscribedTo(topic = "gamepad2/right_trigger/rising")
	public void beaconHalfTransfer(double trigger) {
		boolean rightTriggerPressed = trigger > 0.2;
		if (rightTriggerPressed && !rightTriggerPrevious) {
			orchestrator.startActionAsync("io/intake/beaconHalfTransfer");
		}
		rightTriggerPrevious = rightTriggerPressed;
	}

	@SubscribedTo(topic = "gamepad2/x/rising")
	public void transferToRing() {
		orchestrator.dispatch("io/outtake/target", DepositTarget.POLE);
		orchestrator.startActionAsync("io/transfer");
	}

	@SubscribedTo(topic = "gamepad1/right_bumper/rising")
	public void attemptConeUntip() {
		orchestrator.startActionAsync("io/intake/untip");
	}

	@SubscribedTo(topic = "gamepad1/left_bumper/rising")
	public void alignForUntip() {
		orchestrator.startActionAsync("io/intake/alignForUntip");
	}

	@SubscribedTo(topic = "gamepad2/y/rising")
	public void groundJunctionTransfer() {
		orchestrator.dispatch("io/outtake/target", DepositTarget.GROUND_JUNCTION);
		orchestrator.startActionAsync("io/transfer");
	}

	@SubscribedTo(topic = "gamepad2/dpad_down/rising")
	public void depositLowPole() {
		orchestrator.dispatchSynchronous("io/outtake/target", DepositTarget.LOW_POLE);
		orchestrator.startActionAsync("io/deposit");
	}

	@SubscribedTo(topic = "gamepad2/dpad_right/rising")
	public void depositMiddlePole() {
		orchestrator.dispatchSynchronous("io/outtake/target", DepositTarget.MEDIUM_POLE);
		orchestrator.startActionAsync("io/deposit");
	}

	@SubscribedTo(topic = "gamepad2/dpad_up/rising")
	public void depositHighPole() {
		orchestrator.dispatchSynchronous("io/outtake/target", DepositTarget.HIGH_POLE);
		orchestrator.startActionAsync("io/deposit");
	}

	@SubscribedTo(topic = "gamepad2/a/rising")
	public void releaseAndReset() {
		orchestrator.startActionAsync("io/deposit");
	}
}