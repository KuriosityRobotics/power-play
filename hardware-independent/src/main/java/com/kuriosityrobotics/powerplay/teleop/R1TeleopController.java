package com.kuriosityrobotics.powerplay.teleop;

import static com.kuriosityrobotics.powerplay.math.MathUtil.angleWrap;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.control.FeedForwardPID;
import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Matrices;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.util.Instant;

/**
 * This class subscribes to various gamepad events and then publishes the appropriate messages to
 * the appropriate mechanism/drivetrain topics
 */
public class R1TeleopController extends Node {
	private static final double MAX_MOTOR_VOLTAGE = 12.0;

	@LastMessagePublished(topic = "gamepad1/left_stick_x")
	private double left_stick_x;

	@LastMessagePublished(topic = "gamepad1/left_stick_y")
	private double left_stick_y;

	@LastMessagePublished(topic = "gamepad1/right_stick_x")
	private double right_stick_x;

	@LastMessagePublished(topic = "localisation")
	private LocalisationDatum localisationDatum;

	enum Side {
		RED, BLUE
	}

	private Side driverSide;
	private boolean fieldCentricEnabled = false;
	private boolean yPressedLastFrame = false;

	@SubscribedTo(topic = "gamepad1/x")
	void setBlue() {
		driverSide = Side.BLUE;
		telemetry("CURRENT SIDE", driverSide);
	}

	@SubscribedTo(topic = "gamepad1/b")
	void setRed() {
		driverSide = Side.RED;
		telemetry("CURRENT SIDE", driverSide);
	}

	/**
	 * Converts the gamepad input into motor voltages
	 *
	 * @param xMov    the x-axis movement of the left joystick
	 * @param yMov    the y-axis movement of the left joystick
	 * @param turnMov the x-axis movement of the right joystick
	 * @return the motor voltages to send to the drivetrain
	 */
	private MotorPowers movementsToMotorVoltages(
		double xMov, double yMov, double turnMov) {
		if (localisationDatum != null && driverSide != null && fieldCentricEnabled) {
			var angle = localisationDatum.pose().orientation();
			if (driverSide == Side.RED)
				angle += PI;

			var sinAngle = sin(angle);
			var cosAngle = cos(angle);
			var yMov_ = cosAngle * yMov + sinAngle * xMov;
			var xMov_ = -sinAngle * yMov + cosAngle * xMov;

			telemetry("xMov, xMov_", xMov + ", " + xMov_);
			telemetry("yMov, yMov_", yMov + ", " + yMov_);
			xMov = xMov_;
			yMov = yMov_;
		}
		return MotorPowers.ofPowers(
			(xMov + yMov - turnMov),
			(xMov - yMov + turnMov),
			(xMov - yMov - turnMov),
			(xMov + yMov + turnMov));
	}

	@SubscribedTo(topic = "gamepad1/y")
	public void toggleFieldCentric(boolean pressed){
		if(pressed && !yPressedLastFrame){
			fieldCentricEnabled = !fieldCentricEnabled;
		}
		yPressedLastFrame = pressed;
	}

	/**
	 * This class subscribes to various gamepad events and then publishes the appropriate messages
	 * to the appropriate mechanism/drivetrain topics
	 */
	public R1TeleopController(Orchestrator orchestrator) {
		super(orchestrator);
		err("cope fr");
	}

	@SubscribedTo(topic = "gamepad1/left_stick_x")
	@SubscribedTo(topic = "gamepad1/left_stick_y")
	@SubscribedTo(topic = "gamepad1/right_stick_x")
	public void updateMotorVoltages() {
		orchestrator.dispatch(
			"motorPowers",
			movementsToMotorVoltages(-Math.signum(left_stick_y) * Math.pow(Math.abs(left_stick_y), 13 / 7.), Math.signum(left_stick_x) * Math.pow(Math.abs(left_stick_x), 13 / 7.), -Math.signum(right_stick_x) * Math.pow(Math.abs(right_stick_x), 13 / 7.))
		);
	}

	private final FeedForwardPID xPid = new FeedForwardPID(0, .014, 0, 0);
	private final FeedForwardPID yPid = new FeedForwardPID(0, .014, 0, 0);
	private final FeedForwardPID headingPid = new FeedForwardPID(0, 0, 0, 0);

//
//	@RunPeriodically(maxFrequency = 20)
//	private void brake() {
//		if (localisationDatum != null &&
//			abs(left_stick_x) < .01 &&
//			abs(left_stick_y) < .01 &&
//			abs(right_stick_x) < .01
//		) {
//			orchestrator.dispatch(
//				"motorPowers",
//				movementsToMotorVoltages(
//					xPid.calculateSpeed(0, -localisationDatum.twist().x()),
//					yPid.calculateSpeed(0, -localisationDatum.twist().y()),
//					headingPid.calculateSpeed(0, -localisationDatum.twist().angular())
//				)
//			);
//		}
//	}

	@SubscribedTo(topic = "gamepad2/a")
	public void releaseAndReset(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("outtake/release", "");
		}
	}
	@SubscribedTo(topic = "gamepad2/y")
	public void grabConeIntake(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("outtake/highJunction", "");
		}
	}

	int height = 0, angle = 0;
	double[] heights = {11, 15, 18.3, 20};
	double[] angles = {toRadians(15), toRadians(25), toRadians(155.3), toRadians(170)};
	double setAngle = 0.;
	double angleMultiplier = 1;

	@SubscribedTo(topic = "gamepad2/dpad_up")
	public void incrementAngle(boolean pressed) {
		if (pressed) {
			//setAngle += toRadians(10);
			//orchestrator.dispatch("outtake/setpoint/angle", setAngle);
		}
	}

	@SubscribedTo(topic = "gamepad2/dpad_down")
	public void decrementAngle(boolean pressed) {
		if (pressed) {
			//setAngle -= toRadians(10);
			//orchestrator.dispatch("outtake/setpoint/angle", setAngle);
		}
	}

	@SubscribedTo(topic = "gamepad2/dpad_left")
	public void groundJunction(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("outtake/ground", "");
		}
	}

	@SubscribedTo(topic = "gamepad2/dpad_down")
	public void shortJunction(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("outtake/shortJunction", "");
		}
	}

	@SubscribedTo(topic = "gamepad2/dpad_right")
	public void mediumJunction(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("outtake/medJunction", "");
		}
	}

	@SubscribedTo(topic = "gamepad2/dpad_up")
	public void highJunction(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("outtake/highJunction", "");
		}
	}

	@SubscribedTo(topic = "gamepad2/x")
	public void intakeFront(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("intake/front", "");
		}
	}

	@SubscribedTo(topic = "gamepad2/b")
	public void intakeBack(boolean pressed) {
		if (pressed) {
			orchestrator.dispatch("intake/back", "");
		}
	}
}
