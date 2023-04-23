package com.kuriosityrobotics.powerplay.physics;

import com.kuriosityrobotics.powerplay.drive.MotorPowers;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;

/** Does not try to predict acceleration. */
public class NonPredictingDrive extends RobotDynamicsModel {
	@Override
	protected double[] getAcceleration(Pose pose, Twist velocity, MotorPowers inputs) {
		return new double[] {0, 0, 0};
	}
}
