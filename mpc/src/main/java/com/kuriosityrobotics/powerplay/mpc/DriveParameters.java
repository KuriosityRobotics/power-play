package com.kuriosityrobotics.powerplay.mpc;


/**
 * This class is based on the following struct:
	struct drive_parameters {
		double motor_constant;
		double armature_resistance;

		double robot_mass;
		double robot_moment;
		double wheel_moment;
		double roller_moment;

		double fl_wheel_friction;
		double fr_wheel_friction;
		double bl_wheel_friction;
		double br_wheel_friction;

		double fl_roller_friction;
		double fr_roller_friction;
		double bl_roller_friction;
		double br_roller_friction;

		double battery_voltage;
	};
 */


public class DriveParameters {
    public static final int SIZE = 15;

    public final double motor_constant;
    public final double armature_resistance;
    public final double robot_mass;
    public final double robot_moment;
    public final double wheel_moment;
    public final double roller_moment;
    public final double fl_wheel_friction;
    public final double fr_wheel_friction;
    public final double bl_wheel_friction;
    public final double br_wheel_friction;
    public final double fl_roller_friction;
    public final double fr_roller_friction;
    public final double bl_roller_friction;
    public final double br_roller_friction;
    public final double battery_voltage;

    public DriveParameters(
        double motor_constant, double armature_resistance, double robot_mass, double robot_moment,
        double wheel_moment, double roller_moment, double fl_wheel_friction, double fr_wheel_friction,
        double bl_wheel_friction, double br_wheel_friction, double fl_roller_friction,
        double fr_roller_friction, double bl_roller_friction, double br_roller_friction, double battery_voltage
    ) {
        this.motor_constant = motor_constant;
        this.armature_resistance = armature_resistance;
        this.robot_mass = robot_mass;
        this.robot_moment = robot_moment;
        this.wheel_moment = wheel_moment;
        this.roller_moment = roller_moment;
        this.fl_wheel_friction = fl_wheel_friction;
        this.fr_wheel_friction = fr_wheel_friction;
        this.bl_wheel_friction = bl_wheel_friction;
        this.br_wheel_friction = br_wheel_friction;
        this.fl_roller_friction = fl_roller_friction;
        this.fr_roller_friction = fr_roller_friction;
        this.bl_roller_friction = bl_roller_friction;
        this.br_roller_friction = br_roller_friction;
        this.battery_voltage = battery_voltage;
    }

    public static DriveParameters ofDoubleArray(double[] array) {
        if (array.length != SIZE) {
            throw new IllegalArgumentException("Invalid array size. Expected: " + SIZE + ", Actual: " + array.length);
        }
        
        return new DriveParameters(
            array[0], array[1], array[2], array[3], array[4], array[5], array[6], array[7], array[8], array[9],
            array[10], array[11], array[12], array[13], array[14]
        );
    }

    public static DriveParameters ofBatteryVoltage(double battery_voltage, DriveParameters prev_parameters) {
        return new DriveParameters(
            prev_parameters.motor_constant,
            prev_parameters.armature_resistance,
            prev_parameters.robot_mass,
            prev_parameters.robot_moment,
            prev_parameters.wheel_moment,
            prev_parameters.roller_moment,
            prev_parameters.fl_wheel_friction,
            prev_parameters.fr_wheel_friction,
            prev_parameters.bl_wheel_friction,
            prev_parameters.br_wheel_friction,
            prev_parameters.fl_roller_friction,
            prev_parameters.fr_roller_friction,
            prev_parameters.bl_roller_friction,
            prev_parameters.br_roller_friction,
            battery_voltage
        );
    }

    public static DriveParameters ofDefaulDriveParameters(double battery_voltage) {
        return new DriveParameters(0.3, 1.8, 13.35, 1.19, 0.04, 0.002, 0.256, 0.256, 0.256, 0.256, 20.8, 20.8, 20.8, 20.8, battery_voltage);
    }

    public String toString() {
        return String.format("DriveParameters(motor_constant=%.2f, armature_resistance=%.2f, robot_mass=%.2f, robot_moment=%.2f, wheel_moment=%.2f, roller_moment=%.3f, fl_wheel_friction=%.3f, fr_wheel_friction=%.3f, bl_wheel_friction=%.3f, br_wheel_friction=%.3f, fl_roller_friction=%.2f, fr_roller_friction=%.2f, bl_roller_friction=%.2f, br_roller_friction=%.2f, batteryVoltage=%.2f)", 
                motor_constant, armature_resistance, robot_mass, robot_moment, wheel_moment, roller_moment,
                fl_wheel_friction, fr_wheel_friction, bl_wheel_friction, br_wheel_friction, fl_roller_friction,
                fr_roller_friction, bl_roller_friction, br_roller_friction, battery_voltage);
    }

    public double[] toDoubleArray() {
        return new double[]
            {
                motor_constant, armature_resistance, robot_mass, robot_moment, wheel_moment, roller_moment,
                fl_wheel_friction, fr_wheel_friction, bl_wheel_friction, br_wheel_friction,
                fl_roller_friction, fr_roller_friction, bl_roller_friction, br_roller_friction,
                battery_voltage
            };
    }
}