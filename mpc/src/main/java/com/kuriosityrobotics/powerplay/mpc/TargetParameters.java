package com.kuriosityrobotics.powerplay.mpc;

import com.kuriosityrobotics.powerplay.math.Point;


/**
 * This class is based on the following struct:
	struct robot_target_state {
		double position[3];
		double velocity[3];
	};
 */


public class TargetParameters {
    public static final int SIZE = 6;

    public final double x_desired;
    public final double y_desired;
    public final double theta_desired;
    public final double x_vel_desired;
    public final double y_vel_desired;
    public final double theta_vel_desired;

    public TargetParameters(double x, double y, double theta, double xVel, double yVel, double thetaVel) {
        x_desired = x;
        y_desired = y;
        theta_desired = theta;
        x_vel_desired = xVel;
        y_vel_desired = yVel;
        theta_vel_desired = thetaVel;
    }

    public static TargetParameters ofDoubleArray(double[] array) {
        if (array.length != SIZE) {
            throw new IllegalArgumentException("Invalid array size. Expected: " + SIZE + ", Actual: " + array.length);
        }
    
        return new TargetParameters(array[0], array[1], array[2],
            array[3], array[4], array[5]);
    }

    public static TargetParameters ofPointAndAngle(Point point, double angle) {
        return new TargetParameters(point.x(), point.y(), angle, 0, 0, 0);
    }

    public Point getPoint() {
        return new Point(x_desired, y_desired);
    }

    public String toString() {
        return String.format("TargetParameters(x_desired=%.2f, y_desired=%.2f, theta_desired=%.2f, x_vel_desired=%.2f, y_vel_desired=%.2f, theta_vel_desired=%.2f)", 
            x_desired, y_desired, theta_desired, x_vel_desired, y_vel_desired, theta_vel_desired);
    }

    public double[] toDoubleArray() {
        return new double[]
            {
                x_desired, y_desired, theta_desired,
                x_vel_desired, y_vel_desired, theta_vel_desired
            };
    }
}