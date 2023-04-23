package com.kuriosityrobotics.powerplay.mpc;

import static com.kuriosityrobotics.powerplay.mpc.SolverOutput.NUM_STAGES;

import static java.lang.Math.abs;
import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.navigation.Path;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunnableAction;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.util.Instant;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MPCNode extends Node {
	public static final int USE_STAGES = 1;
	private static final double DERIVATIVE_THRESHOLD = .005;


	private final OptimisationParameterBuilder paramsTemplate = new OptimisationParameterBuilder().setLinearWeights(2.7);

	private final ReentrantLock lock = new ReentrantLock();
	private final Condition stateUpdateCondition = lock.newCondition();

	private volatile FollowState state = FollowState.EN_ROUTE;


	private Follower follower;
	private boolean shouldMaintainConstantHeading;


	public MPCNode(Orchestrator orchestrator) throws InterruptedException {
		super(orchestrator);

		lock.lockInterruptibly();
		try {
			// redundant, but helpful
			setAngleTarget(0);
			maintainHeading(false);
			setVelTargets(Twist.zero());
			setLinearVelWeight(0);
			setAngularVelWeight(0);
		} finally {
			lock.unlock();
		}
		info("Constructing MPCNode " + hashCode());
	}

	private boolean flipDirection = false;

	@SubscribedTo(topic = "mpc/flipDirection")
	public void flipLeftRight(boolean flip) {
		flipDirection = flip;
	}


	private MotorPowers processMotorPowers(MotorPowers p) {
		if (flipDirection) {
			return MotorPowers.ofPowers(p.powerFrontRight(), p.powerBackRight(), p.powerFrontLeft(), p.powerBackLeft());
		} else {
			return p;
		}
	}

	@SubscribedTo(topic = "mpc/resetPath")
	public void resetPath(Path path) throws InterruptedException {
		lock.lockInterruptibly();

		try {
			state = FollowState.EN_ROUTE;

			powers.clear();
			this.follower = new Follower(path);
			follower.setShouldMaintainConstantHeading(shouldMaintainConstantHeading);
		} finally {
			lock.unlock();
		}

		info("MPCNode path reset " + hashCode());
	}

	@SubscribedTo(topic = "mpc/setTargetAngle")
	public void setAngleTarget(double angle) {
		paramsTemplate.setAngularTarget(angle);
	}

	@SubscribedTo(topic = "mpc/maintainConstantHeading")
	// when false, it will only care about heading at the end of the path
	public void maintainHeading(boolean maintainHeading) {
		this.shouldMaintainConstantHeading = maintainHeading;
		if (follower != null) {
			follower.setShouldMaintainConstantHeading(maintainHeading);
		}
	}

	@SubscribedTo(topic = "mpc/setVelocity")
	// only used for *collision anticipation*
	public void setVelTargets(Twist velocity) {
		paramsTemplate.setTargetVelocity(velocity.x(), velocity.y(), velocity.angular());
	}

	@SubscribedTo(topic = "mpc/setLinearVelWeight")
	// only used for *collision anticipation* and sets x and y vel weights
	public void setLinearVelWeight(double weight) {
		paramsTemplate.setLinearVelocityWeights(weight);
	}

	@SubscribedTo(topic = "mpc/setAngularVelWeight")
	// only used for *collision anticipation* and sets angular vel weights
	public void setAngularVelWeight(double weight) {
		paramsTemplate.setAngularVelocityWeights(weight);
	}


	private final ConcurrentLinkedDeque<SystemState> powers = new ConcurrentLinkedDeque<>();
	private SystemState lastState = SystemState.ofLocalisation(LocalisationDatum.zero());

	@LastMessagePublished(topic = "localisation")
	private LocalisationDatum l;

	@LastMessagePublished(topic = "batteryVoltage")
	private double batteryVoltage;

	private void runSolver() throws InterruptedException {
		follower.correctDistance(l);
		var parameters = follower.getNextSolverParameters(paramsTemplate);

		var solver = new SolverInputBuilder()
			.withParameters(parameters)
			.withInitialGuess(lastState)
			.startingAt(l)
			.getRange(0, NUM_STAGES, batteryVoltage);

		var start = Instant.now();
		var result = solver.solve();
		var end = Instant.now();

		info("Solved in " + (end.since(start).toMillis()) + "ms");

		if (lock.tryLock()) {
			try {
				double lastStateDerivative = (
					SolverInput.stageObjective(solver.getOptimisationParameters()[NUM_STAGES - 2], result.getStates()[NUM_STAGES - 2])
						- SolverInput.stageObjective(solver.getOptimisationParameters()[0], result.getStates()[0])
				);
				info("lastStateDerivative:  " + lastStateDerivative);
				info("remaining:  " + follower.getDistanceRemaining(follower.distanceAlongPath) / follower.path.pathLength());

				if (-lastStateDerivative < DERIVATIVE_THRESHOLD && follower.getDistanceRemaining(follower.distanceAlongPath) / follower.path.pathLength() < .1)
					setState(FollowState.ARRIVED);
				else
					setState(FollowState.EN_ROUTE);
				for (int i = 0; i < USE_STAGES; i++) {
					var state = result.getStates()[i];
					powers.addLast(state);
				}

				lastState = result.getStates()[USE_STAGES - 1];
			} finally {
				lock.unlock();
			}
		}
	}

	private void setState(FollowState state) throws InterruptedException {
		lock.lockInterruptibly();

		try {
			if (this.state != state) {
				this.state = state;
				stateUpdateCondition.signalAll();
			}
		} finally {
			lock.unlock();
		}
	}

	@RunPeriodically(maxFrequency = 10)
	public void update() throws InterruptedException {
		if (follower == null) {
			return;
		}

		if (powers.isEmpty()) {
			runSolver();
		}
		var nextState = powers.poll();
		var expected = new LocalisationDatum(
			Instant.now(),
			new Pose(nextState.getX(), nextState.getY(), nextState.getTheta()),
			new Twist(nextState.getXVel(), nextState.getYVel(), nextState.getThetaVel()).rotate(-nextState.getTheta())
		);

		info(String.format("%nactual %s%nexpected %s%n%n", l, expected));

		orchestrator.dispatch("motorPowers", nextState.getMotorPowers());

		if (powers.isEmpty()) {
			runSolver();
		}
	}

	@RunnableAction(actionName = "mpc/awaitPathEnd")
	public void followPath() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			while (follower == null || state != FollowState.ARRIVED) {
				info("[a] State " + state);
				info("[a] MPCNode " + this);
				info("[a] Follower " + follower);
				info("[a] Path " + follower.path);
				stateUpdateCondition.await();
			}
		} finally {
			lock.unlock();
		}
	}

	@LastMessagePublished(topic = "mpc/motorWeight")
	private double baseMotorWeight = 0.05;

	public class Follower {
		public static final double DEFAULT_TARGET_DISTANCE = 0.05;


		private final Path path;

		private volatile double distanceAlongPath;
		private boolean shouldMaintainConstantHeading;


		public Follower(Path path) {
			requireNonNull(path);
			this.path = path;
		}

		public void correctDistance(LocalisationDatum datum) throws InterruptedException {
			lock.lockInterruptibly();

			try {
				this.distanceAlongPath = path.closestPointLengthInRange(datum.pose().metres(), distanceAlongPath - 0.1, distanceAlongPath + 0.1);
			} finally {
				lock.unlock();
			}
		}

		public OptimisationParameters[] getNextSolverParameters(OptimisationParameterBuilder template) {
			var distance = distanceAlongPath;
			var parameters = new OptimisationParameters[NUM_STAGES];

			for (int i = 0; i < NUM_STAGES; i++) {
				distance += stepSize(distance);

				parameters[i] = template.clone()
					.setTargetPoint(path.distanceAlong(distance))
					.setAngularWeights(shouldMaintainConstantHeading || getDistanceRemaining(distance) < 0.4 ? 0.1 : 0)
					.setMotorWeights(motorWeight(distance))
					.build(-1);
			}

			return parameters;
		}

		private double stepSize(double distance) {
			return DEFAULT_TARGET_DISTANCE / (0.2 * path.curvature(distance) + 1);
		}

		public double closestPointLength(Point point) {
			return path.closestPointLengthInRange(point, distanceAlongPath - 0.1, distanceAlongPath + 0.1);
		}

		public double getDistanceRemaining(double distance) {
			return path.pathLength() - distance;
		}

		private double motorWeight(double distance) {
			return (baseMotorWeight * (0.4 * path.curvature(distance) + 1)) + (getDistanceRemaining(distance) < 0.2 ? 0.03 : 0.);
		}


		public void setShouldMaintainConstantHeading(boolean shouldMaintainConstantHeading) {
			this.shouldMaintainConstantHeading = shouldMaintainConstantHeading;
		}
	}

}