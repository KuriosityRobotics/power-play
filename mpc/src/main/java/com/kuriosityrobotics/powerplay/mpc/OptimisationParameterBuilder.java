package com.kuriosityrobotics.powerplay.mpc;

import java.util.Arrays;
import com.kuriosityrobotics.powerplay.math.Point;

public class OptimisationParameterBuilder {
	private double[] arguments = new double[OptimisationParameters.SIZE];

	public OptimisationParameterBuilder() {
		setDefaultDriveModel(); // sets defaults unless copied from already-existing parameters
	}

	public OptimisationParameterBuilder(OptimisationParameters parameters) {
		System.arraycopy(parameters.toDoubleArray(), 0, arguments, 0, 31);
	}

	public OptimisationParameterBuilder(OptimisationParameterBuilder builder) {
		System.arraycopy(builder.arguments, 0, arguments, 0, OptimisationParameters.SIZE);
	}

	public OptimisationParameterBuilder setMotorWeights(double weight) {
		Arrays.fill(arguments, 21, 25, weight);
		return this;
	}

	public OptimisationParameterBuilder setLinearWeights(double weight) {
		Arrays.fill(arguments, 25, 27, weight);
		return this;
	}

	public OptimisationParameterBuilder setAngularWeights(double weight) {
		arguments[27] = weight;
		return this;
	}

	public OptimisationParameterBuilder setLinearTargets(double xTarget, double yTarget) {
		arguments[15] = xTarget;
		arguments[16] = yTarget;
		return this;
	}

	public OptimisationParameterBuilder setTargetPoint(Point target) {
		setLinearTargets(target.x(), target.y());
		return this;
	}

	public OptimisationParameterBuilder setAngularTarget(double angleTarget) {
		arguments[17] = angleTarget;
		return this;
	}

	public OptimisationParameterBuilder setLinearVelocityWeights(double weight) {
		Arrays.fill(arguments, 28, 30, weight);
		return this;
	}

	public OptimisationParameterBuilder setAngularVelocityWeights(double weight) {
		arguments[30] = weight;
		return this;
	}

	public OptimisationParameterBuilder setLinearVelocityTargets(double xVelocityTarget, double yVelocityTarget) {
		arguments[18] = xVelocityTarget;
		arguments[19] = yVelocityTarget;
		return this;
	}

	public OptimisationParameterBuilder setAngularVelocityTarget(double angularVelocityTarget) {
		arguments[20] = angularVelocityTarget;
		return this;
	}

	public OptimisationParameterBuilder setTargetPosition(double xTarget, double yTarget, double angleTarget) {
		setLinearTargets(xTarget, yTarget);
		setAngularTarget(angleTarget);
		return this;
	}

	public OptimisationParameterBuilder setTargetVelocity(double xVelocityTarget, double yVelocityTarget, double angularVelocityTarget) {
		setLinearVelocityTargets(xVelocityTarget, yVelocityTarget);
		setAngularVelocityTarget(angularVelocityTarget);
		return this;
	}

	public OptimisationParameterBuilder setDefaultDriveModel() {
		double[] defaultModel = new double[] {0.3, 1.8, 13.35, 1.19, 0.04, 0.002, 0.256, 0.256, 0.256, 0.256, 20.8, 20.8, 20.8, 20.8};
		System.arraycopy(defaultModel, 0, arguments, 0, defaultModel.length);
		return this;
	}

	public OptimisationParameterBuilder setDriveParameters(DriveParameters parameters) {
		System.arraycopy(parameters.toDoubleArray(), 0, arguments, 0, 15);
		return this;
	}

	public OptimisationParameterBuilder setTargetParameters(TargetParameters parameters) {
		System.arraycopy(parameters.toDoubleArray(), 0, arguments, 15, 6);
		return this;
	}

	public OptimisationParameterBuilder setWeightParameters(WeightParameters parameters) {
		System.arraycopy(parameters.toDoubleArray(), 0, arguments, 21, 10);
		return this;
	}

	public OptimisationParameters build(double batteryVoltage) {
		arguments[14] = batteryVoltage;

		return new OptimisationParameters(
			DriveParameters.ofDoubleArray(Arrays.copyOfRange(arguments, 0, 15)),
			TargetParameters.ofDoubleArray(Arrays.copyOfRange(arguments, 15, 21)),
			WeightParameters.ofDoubleArray(Arrays.copyOfRange(arguments, 21, 31))
		);
	}

	public OptimisationParameterBuilder clone() {
		return new OptimisationParameterBuilder(this);
	}
}
