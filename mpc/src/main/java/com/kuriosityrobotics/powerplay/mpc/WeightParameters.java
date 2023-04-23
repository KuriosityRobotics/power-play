package com.kuriosityrobotics.powerplay.mpc;

/**
 * This class is based on the following struct:
	struct objective_weights {
		double motor_weights[4];
		double robot_position_weights[3];
		double robot_velocity_weights[3];
	};
 */

 public class WeightParameters {
    public static final int SIZE = 10;

    public double fl_weight;
    public double fr_weight;
    public double bl_weight;
    public double br_weight;

    public final double x_weight;
    public final double y_weight;
    public final double theta_weight;

    public final double x_vel_weight;
    public final double y_vel_weight;
    public final double theta_vel_weight;

    public WeightParameters(
        double flWeight, double frWeight, double blWeight, double brWeight,
        double xWeight, double yWeight, double thetaWeight,
        double xVelWeight, double yVelWeight, double thetaVelWeight
    ) {
        this.fl_weight = flWeight;
        this.fr_weight = frWeight;
        this.bl_weight = blWeight;
        this.br_weight = brWeight;

        this.x_weight = xWeight;
        this.y_weight = yWeight;
        this.theta_weight = thetaWeight;

        this.x_vel_weight = xVelWeight;
        this.y_vel_weight = yVelWeight;
        this.theta_vel_weight = thetaVelWeight;
    }

    public static WeightParameters ofDoubleArray(double[] array) {
        if (array.length != SIZE) {
            throw new IllegalArgumentException("Invalid array size. Expected: " + SIZE + ", Actual: " + array.length);
        }

        return new WeightParameters(
            array[0], array[1], array[2], array[3],
            array[4], array[5], array[6],
            array[7], array[8], array[9]);
    }

    public static WeightParameters ofUniformWeights(
        double motor_weight,
        double position_weight, double theta_weight,
        double velocity_weight, double theta_velocity_weight
    ) {
        return new WeightParameters(
            motor_weight, motor_weight, motor_weight, motor_weight,
            position_weight, position_weight, theta_weight,
            velocity_weight, velocity_weight, theta_velocity_weight
        );
    }

    public String toString() {
        return String.format(
            "WeightParameters(fl_weight=%.2f, fr_weight=%.2f, bl_weight=%.2f, br_weight=%.2f, x_weight=%.2f, y_weight=%.2f, theta_weight=%.2f, x_vel_weight=%.2f, y_vel_weight=%.2f, theta_vel_weight=%.2f)",
            fl_weight, fr_weight, bl_weight, br_weight, x_weight, y_weight, theta_weight, x_vel_weight, y_vel_weight, theta_vel_weight);
    }

    public double[] toDoubleArray() {
        return new double[]
            {
                fl_weight, fr_weight, bl_weight, br_weight,
                x_weight, y_weight, theta_weight,
                x_vel_weight, y_vel_weight, theta_vel_weight
            };
    }
}