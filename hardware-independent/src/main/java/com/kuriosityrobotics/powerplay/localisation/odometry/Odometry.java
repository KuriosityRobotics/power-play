package com.kuriosityrobotics.powerplay.localisation.odometry;

import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.WHEEL_RADIUS;
import static com.kuriosityrobotics.powerplay.localisation.LocalisationUtil.covarianceFromStandardDeviation;
import static com.kuriosityrobotics.powerplay.util.Units.CM;

import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import com.kuriosityrobotics.powerplay.bulkdata.RevHubBulkData;
import com.kuriosityrobotics.powerplay.localisation.messages.TimedTwistWithCovariance;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.util.Instant;

import org.ojalgo.matrix.Primitive64Matrix;

import java.util.Map;
import java.util.Queue;

/**
 * The <code>Odometry</code> class is a node that takes in a stream of {@link RevHubBulkData} messages and
 * publishes a stream of {@link TimedTwistWithCovariance} messages containing the robot's velocity.
 */
public class Odometry extends Node {
	public static final int FORWARDS_ODO_PORT = 2;
	public static final int SIDEWAYS_ODO_PORT = 0;

	public static final double DISTANCE_TO_FORWARDS_ENCODER = 8.2767012 * CM; // distance leftwards from center of robot to forwards encoder (metres)
	public static final double DISTANCE_TO_SIDEWAYS_ENCODER = -7.50815928 * CM; // distance forwards from center of robot to sideways encoder (metres)

	public Odometry(Orchestrator orchestrator) {
		super(orchestrator);
	}

	@LastMessagePublished(topic = "angular_vel")
	private double angularVel;


	private volatile double frontLeftVel;

	@SubscribedTo(topic = "expansionHub/encoder/" + FORWARDS_ODO_PORT + "/velocity")
	private void updateFl(double value) {
		frontLeftVel = value;
	}

	private volatile double backVel;

	@SubscribedTo(topic = "expansionHub/encoder/" + SIDEWAYS_ODO_PORT + "/velocity")
	private void updateBack(double value) {
		backVel = -value;
	}

	@SubscribedTo(topic = "controlHub")
	private void calculateOdometry() {
		var time = Instant.now();
		Primitive64Matrix covariance = covarianceFromStandardDeviation(.137, .137, .05);

		var velocity = calculateOdometryRel(frontLeftVel, backVel);
		orchestrator.dispatch("velocity",
			TimedTwistWithCovariance.of(
				velocity,
				covariance,
				time
			));
	}

	private Twist calculateOdometryRel(double velocityFrontLeft, double velocityBackCentre) {
		var angularVel = this.angularVel;

		double relativeVelForwards = WHEEL_RADIUS * velocityFrontLeft + DISTANCE_TO_FORWARDS_ENCODER * angularVel;
		double relativeVelSideways = WHEEL_RADIUS * velocityBackCentre - DISTANCE_TO_SIDEWAYS_ENCODER * angularVel;

		return new Twist(relativeVelForwards, relativeVelSideways, angularVel);
	}
}
