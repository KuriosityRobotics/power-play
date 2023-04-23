package com.kuriosityrobotics.powerplay.physics;

import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;

/** A model that describes the dynamics of a robot, given its input motor voltages */
public abstract class RobotDynamicsModel {
	/**
	 * Returns a vector containing the derivatives with respect to time of the pose and velocity of
	 * the robot
	 *
	 * @param pose the current pose
	 * @param velocity the current velocity
	 * @param inputs the current motor voltages
	 * @return a vector containing the derivatives with respect to time of the pose and velocity of
	 *     the robot. [in/s, in/s, rad/s, in/s^2, in/s^2, rad/s^2]
	 */
	public final double[] getDerivatives(Pose pose, Twist velocity, MotorPowers inputs) {
		var output = new double[6]; // grad<pose, velo> = <velo, accel>
		output[0] = velocity.x(); // dX/dt
		output[1] = velocity.y(); // dY/dt
		output[2] = velocity.angular(); // dS/dt

		var accel = getAcceleration(pose, velocity, inputs);
		output[3] = accel[0]; // d^2X/dt^2
		output[4] = accel[1]; // d^2Y/dt^2
		output[5] = accel[2]; // d^2S/dt^2

		return output;
	}

	/**
	 * Returns the acceleration of the robot, given the current pose, velocity, and motor voltages
	 *
	 * @param pose the current pose
	 * @param velocity the current velocity
	 * @param inputs the current motor voltages
	 * @return acceleration of the robot [in/s/s, in/s/s, rad/s/s]
	 */
	protected abstract double[] getAcceleration(Pose pose, Twist velocity, MotorPowers inputs);
}
