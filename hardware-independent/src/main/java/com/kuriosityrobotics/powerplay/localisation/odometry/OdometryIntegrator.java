package com.kuriosityrobotics.powerplay.localisation.odometry;

import static java.lang.Math.toDegrees;

import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.localisation.messages.TimedTwistWithCovariance;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.util.Instant;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class OdometryIntegrator extends Node {
	private final AtomicReference<Pose> state;

	private Instant lastUpdateTime;

	public OdometryIntegrator(Orchestrator orchestrator, Pose initialPose) {
		super(orchestrator);
		state = new AtomicReference<>(initialPose);
		lastUpdateTime = Instant.now();
	}

	public OdometryIntegrator(Orchestrator orchestrator) {
		super(orchestrator);
		state = new AtomicReference<>(new Pose(0, 0, 0));
		lastUpdateTime = Instant.now();
	}

	private Twist relToGlobalVel(Pose state, Twist relativeVelocity) {
		var orientation = state.orientation();
		double velocityX = Math.cos(orientation) * relativeVelocity.getX() - Math.sin(orientation) * relativeVelocity.getY();
		double velocityY = Math.sin(orientation) * relativeVelocity.getX() + Math.cos(orientation) * relativeVelocity.getY();
		double velocityAngle = relativeVelocity.angular();
		return new Twist(velocityX, velocityY, velocityAngle);
	}

	@SubscribedTo(topic = "angle")
	private void updateAngle(double angle) {
		state.getAndUpdate(state -> new Pose(state.x(), state.y(), angle));
	}

	@SubscribedTo(topic = "velocity")
	private void updateOdometry(TimedTwistWithCovariance ttwc) {
		double dt = ttwc.time().since(lastUpdateTime).toSeconds();

		state.getAndUpdate(state -> {
			state = state.add(relToGlobalVel(state, ttwc).scalarMultiply(dt));
			lastUpdateTime = ttwc.time();

			orchestrator.dispatch("localisation",
				new LocalisationDatum(
					lastUpdateTime, state, ttwc)
			);
			return state;
		});
	}

	@SubscribedTo(topic = "localisation/reset-position")
	private void resetPosition(Pose resetPose) {
		state.set(resetPose);
	}

	public Pose getPose() {
		return state.get();
	}
}
