package com.kuriosityrobotics.powerplay.mpc;

import static java.lang.Math.pow;

import com.kuriosityrobotics.powerplay.math.Point;

import java.util.Arrays;

/**
 * This class is based on the following struct and serves as a combination of three components:
	struct optimisation_parameters {
		struct drive_parameters model;
		struct robot_target_state target;
		struct objective_weights weights;
	};
 */

public final class OptimisationParameters {
	private static final double NOMINAL_BATTERY = 13.5;
	public static final int SIZE = WeightParameters.SIZE + DriveParameters.SIZE + TargetParameters.SIZE; // 31

	public final DriveParameters model;
	public final TargetParameters target;
	public final WeightParameters weights;
	
	public OptimisationParameters(DriveParameters model, TargetParameters target, WeightParameters weights){
		this.model = model;
		this.target = target;
		this.weights = weights;
	}

	public static OptimisationParameters ofBatteryVoltage(double batteryVoltage, OptimisationParameters prevParams) {
		double[] arguments = prevParams.toDoubleArray();
		arguments[14] = batteryVoltage;

		var weights = WeightParameters.ofDoubleArray(Arrays.copyOfRange(arguments, 21, 31));
		var batteryAdjustment = pow(batteryVoltage / NOMINAL_BATTERY, 2);

		weights.fl_weight *= batteryAdjustment;
		weights.fr_weight *= batteryAdjustment;
		weights.bl_weight *= batteryAdjustment;
		weights.br_weight *= batteryAdjustment;


		return new OptimisationParameters(
			DriveParameters.ofDoubleArray(Arrays.copyOfRange(arguments, 0, 15)),
			TargetParameters.ofDoubleArray(Arrays.copyOfRange(arguments, 15, 21)),
			weights
		);
	}

	public String toString() {
		return String.format("OptimisationParameters(\nmodel=%s,\ntarget=%s, \nweights=%s\n)",
				model, target, weights);
	}

	// concatenate all the objective terms into a single array so that it has the same memory layout as the C struct
	// formatted [DriveModel, Targets, Weights] (31 elements)
	public double[] toDoubleArray() {
		double[] output = new double[SIZE];
		double[][] arrays = {model.toDoubleArray(), target.toDoubleArray(), weights.toDoubleArray()};

		int i = 0;
		for (double[] array : arrays) {
			System.arraycopy(array, 0, output, i, array.length);
			i += array.length;
		}
		
		return output;
	}

}
