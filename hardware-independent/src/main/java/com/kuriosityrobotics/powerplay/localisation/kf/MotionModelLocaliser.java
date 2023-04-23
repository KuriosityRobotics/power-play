package com.kuriosityrobotics.powerplay.localisation.kf;

import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.localisation.messages.TimedTwistWithCovariance;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.PoseWithCovariance;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.math.TwistWithCovariance;
import com.kuriosityrobotics.powerplay.physics.MecanumDrive;
import com.kuriosityrobotics.powerplay.physics.RobotDynamicsModel;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.util.Instant;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.ojalgo.matrix.Primitive64Matrix;

// TODO:  documentation for this class
public class MotionModelLocaliser extends Node {
	private static final Primitive64Matrix stateToVelocity =
		Primitive64Matrix.FACTORY.rows(
			new double[][]{
				{0, 0, 0, 1, 0, 0},
				{0, 0, 0, 0, 1, 0},
				{0, 0, 0, 0, 0, 1}
			});

	private final ExtendedKalmanFilter extendedKalmanFilter;

	@LastMessagePublished(topic = "motorPowers")
	private MotorPowers motorPowers = MotorPowers.ofPowers(0, 0, 0, 0);

	private final RobotDynamicsModel dynamics;

	private Instant lastUpdateTime;

	public MotionModelLocaliser(Orchestrator orchestrator, RobotDynamicsModel model) {
		this(orchestrator, model, 0, 0, 0, 0, 0, 0);
	}

	MotionModelLocaliser(
		Orchestrator orchestrator, RobotDynamicsModel dynamics, double... initialState) {
		super(orchestrator);
		this.dynamics = dynamics;

		this.extendedKalmanFilter = new ExtendedKalmanFilter(initialState, 0, 0, 0, .1, .1, .1);
		lastUpdateTime = Instant.now();
	}

	/**
	 * Incorporates a TwistWithCovariance (velocity) measurement into the Kalman Filter
	 *
	 * @param twc the TwistWithCovariance measurement
	 */
	@SubscribedTo(topic = "velocity")
	private void correctVelocity(TimedTwistWithCovariance twc) {
		extendedKalmanFilter
			.builder()
			.covariance(twc.covariance())
			.mean(twc.getData())
			.stateToOutput(stateToVelocity)
			.time(twc.time().toEpochMilli()) // TODO: fix
			.correct();
	}

	/**
	 * Uses the com.kuriosityrobotics.powerplay.physics model to predict the robot's current
	 * position pose += velocity * dt velocity += physicsModel.getAcceleration() * dt
	 */
	@RunPeriodically(maxFrequency = 300)
	private void forwardPrediction() {
		var currentTime = Instant.now();

		var dt = lastUpdateTime.until(currentTime).toMillis() / 1000.;

		var derivatives = dynamics.getDerivatives(getPose(), getVelocity(), motorPowers);

		extendedKalmanFilter
			.builder()
			.mean(derivatives)
			.covariance(Primitive64Matrix.FACTORY.makeIdentity(6).multiply(0))
			.outputToState(Primitive64Matrix.FACTORY.makeIdentity(6).multiply(dt))
			.predict();
		lastUpdateTime = currentTime;

		orchestrator.dispatch("localisation",
			new LocalisationDatum(
				Instant.now(), getPoseWithCovariance(), getTwistWithCovariance())
		);
	}

	/**
	 * @return current estimated pose
	 */
	public Pose getPose() {
		var output = extendedKalmanFilter.outputVector();
		return new Pose(output[0], output[1], output[2]);
	}

	/**
	 * @return current estimated velocity
	 */
	public Twist getTwist() {
		var output = extendedKalmanFilter.outputVector();
		return new Twist(output[3], output[4], output[5]);
	}

	/**
	 * @return current estimated pose and its covariance
	 */
	public PoseWithCovariance getPoseWithCovariance() {
		var covariance = extendedKalmanFilter.covariance().limits(3, 3);

		assertThat(
			covariance.getRowDim() == 3,
			"covariance matrix rows must be 3, but was " + covariance.getRowDim());
		assertThat(
			covariance.getColDim() == 3,
			"covariance matrix columns must be 3, but was " + covariance.getColDim());

		return PoseWithCovariance.of(getPose(), covariance);
	}

	/**
	 * @return current estimated velocity and its covariance
	 */
	public TwistWithCovariance getTwistWithCovariance() {
		var covariance = extendedKalmanFilter.covariance().offsets(3, 3).limits(3, 3);

		assertThat(covariance.getRowDim() == 3);
		assertThat(covariance.getColDim() == 3);

		return TwistWithCovariance.of(getTwist(), covariance);
	}

	public Twist getVelocity() {
		var output = extendedKalmanFilter.outputVector();
		return new Twist(output[3], output[4], output[5]);
	}
}
