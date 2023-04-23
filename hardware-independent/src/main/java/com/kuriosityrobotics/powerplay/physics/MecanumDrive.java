package com.kuriosityrobotics.powerplay.physics;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.math.Matrices;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.math.Wrench;
import com.kuriosityrobotics.powerplay.pubsub.LastValue;
import com.kuriosityrobotics.powerplay.pubsub.LogInterface;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;

import org.ojalgo.matrix.Primitive64Matrix;

/** The dynamics of a 4-omni-wheeled robot with NevaRest Orbital 20 motors. */
public class MecanumDrive extends RobotDynamicsModel {
	private final LogInterface logInterface;

	private final LastValue<Double> batteryVoltage;

	private static final double IN_TO_M = .0254, MM_TO_M = .001, G_TO_KG = 1 / 1000.;
	private static final double L = 7.802 * IN_TO_M, l = 4.822 * IN_TO_M;
	private static final double WHEEL_DIAMETER = 25 * MM_TO_M;
	private static final double ROBOT_MASS = 12.24;
	private static final double ROBOT_INERTIA =
			0.5
					* ROBOT_MASS
					* (L * L + l * l); // github copilot generated this calculation, but it's
	// literally 1 gram off
	// of what fusion 360 says for the dt
	private static final double MECANUM_WHEEL_INERTIA = .3712 * G_TO_KG;

	private static final Primitive64Matrix COLLAPSE_90 =
			Primitive64Matrix.FACTORY.rows(
					new double[][] {
						{0, 1, 0},
						{-1, 0, 0},
						{0, 0, 0}
					});

	private static final Primitive64Matrix R =
			Primitive64Matrix.FACTORY
					.rows(
							new double[][] {
								{1, -1, -(L + l)},
								{1, 1, (L + l)},
								{1, 1, -(L + l)},
								{1, -1, (L + l)}
							})
					.multiply(1 / WHEEL_DIAMETER);

	private static final Motor NEVEREST_20 = Motor.neveRestOrbital20();

	public MecanumDrive(Orchestrator orchestrator) {
		this.logInterface = orchestrator;
		this.batteryVoltage = orchestrator.lastValue("batteryVoltage", Double.class);
		batteryVoltage.setDefaultValue(14.);
	}

	@Override
	protected double[] getAcceleration(Pose pose, Twist velocity, MotorPowers inputs) {
		var undoRotation = Matrices.rotationMatrix(pose.orientation()).transpose();

		var wheelSpeeds = R.multiply(undoRotation);
		wheelSpeeds = wheelSpeeds.multiply(velocity.toColumnVectorMetres());

		var wheelTorques = NEVEREST_20.torque(inputs.toColumnVector().multiply(batteryVoltage.getValue()), wheelSpeeds);
		logInterface.telemetry("physics/wheelTorques/w1", wheelTorques.toRawCopy1D()[0]);
		logInterface.telemetry("physics/wheelTorques/w2", wheelTorques.toRawCopy1D()[1]);
		logInterface.telemetry("physics/wheelTorques/w3", wheelTorques.toRawCopy1D()[2]);
		logInterface.telemetry("physics/wheelTorques/w4", wheelTorques.toRawCopy1D()[3]);

		var forces = robotForce(wheelTorques, pose.orientation());

		var H =
				Matrices.diag(ROBOT_MASS, ROBOT_MASS, ROBOT_INERTIA)
						.add(
								Matrices.diag(1, 1, (L + l) * (L + l))
										.multiply(
												4
														* MECANUM_WHEEL_INERTIA
														/ (WHEEL_DIAMETER * WHEEL_DIAMETER)));
		var K =
				COLLAPSE_90.multiply(
						(4 * MECANUM_WHEEL_INERTIA / (WHEEL_DIAMETER * WHEEL_DIAMETER))
								* velocity.angular());
		var accel =
				H.invert()
						.multiply(
								forces.toColumnVector()
										.subtract(K.multiply(velocity.toColumnVectorMetres())));

		var result = accel.toRawCopy1D();
		result[0] /= IN_TO_M;
		result[1] /= IN_TO_M;
		return result;
	}

	/**
	 * Returns the force on the robot, given the torque on each wheel and the current orientation of
	 * the robot
	 *
	 * @param torque the torque on each wheel
	 * @param heading the current orientation of the robot
	 * @return the force on the robot [x, y, theta]
	 */
	private Wrench robotForce(Primitive64Matrix torque, double heading) {
		var forwards = torque.get(0) + torque.get(1) + torque.get(2) + torque.get(3);
		var strafe = torque.get(0) - torque.get(1) - torque.get(2) + torque.get(3);

		return Wrench.of(
				(cos(heading) * forwards + sin(heading) * strafe) * (1 / WHEEL_DIAMETER),
				(sin(heading) * forwards - cos(heading) * strafe) * (1 / WHEEL_DIAMETER),
				(-(L + l) * (torque.get(0) - torque.get(1) + torque.get(2) - torque.get(3)))
						* (1 / WHEEL_DIAMETER));
	}
}
