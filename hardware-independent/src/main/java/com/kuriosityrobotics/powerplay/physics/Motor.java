package com.kuriosityrobotics.powerplay.physics;

import static java.lang.Math.pow;
import static java.lang.Math.signum;

import org.ojalgo.matrix.Primitive64Matrix;

/** This class models the dynamics of an armature-controlled DC motor. */
public class Motor {
	private static final double RPM_TO_RADS = 2 * Math.PI / 60;

	private final double module;
	private final double reduction;
	private final double nominalVoltage;

	private final double armatureResistance;
	private final double motorConstant;

	protected Motor(
			double module,
			double reduction,
			double nominalVoltage,
			double armatureResistance,
			double motorConstant) {
		this.module = module;
		this.reduction = reduction;
		this.nominalVoltage = nominalVoltage;
		this.armatureResistance = armatureResistance;
		this.motorConstant = motorConstant;
	}

	protected Motor(
			double module,
			double reduction,
			double nominalVoltage,
			double stallCurrent,
			double freeSpeed,
			double freeCurrent) {
		this(
				module,
				reduction,
				nominalVoltage,
				nominalVoltage / stallCurrent,
				(nominalVoltage - (nominalVoltage / stallCurrent) * freeCurrent)
						/ (freeSpeed * (nominalVoltage / stallCurrent)));
	}

	/**
	 * @return a {@link Motor} for the NeveRest Orbital 20 gearmotor.
	 * @see <a href="https://www.andymark.com/products/neverest-orbital-20-gearmotor">NeveRest
	 *     Orbital 20 Gearmotor</a>
	 */
	public static Motor neveRestOrbital20() {
		return new Motor(.48, 19.2, 12, 11.7, 6600 * RPM_TO_RADS, .4);
	}

	/**
	 * Returns the friction torque in Nm.
	 *
	 * @param speed the motor speed in rad/s
	 * @return the friction torque in Nm
	 */
	public double friction(double speed) {
		return .35 * signum(speed);
	}

	/**
	 * Returns the friction torque in Nm (vectorised)
	 *
	 * @param speeds the motor speeds in rad/s
	 * @return the friction torques in Nm
	 */
	public Primitive64Matrix friction(Primitive64Matrix speeds) {
		return speeds.multiply(friction(1.));
	}

	/**
	 * Returns the motor torque in Newton-metres.
	 *
	 * @param voltage the voltage of the motor
	 * @param speed the speed of the motor
	 * @return the torque of the motor (Nm)
	 */
	public double torque(double voltage, double speed) {
		return (voltage * module * reduction * motorConstant / armatureResistance)
				- (speed * module * pow(reduction, 2) * pow(motorConstant, 2) / armatureResistance)
				- friction(speed);
	}

	/**
	 * Returns the motor torque in Newton-metres (vectorised)
	 *
	 * @param voltages the voltages of the motors
	 * @param speeds the speeds of the motors
	 * @return the torque of the motors (Nm)
	 */
	public Primitive64Matrix torque(Primitive64Matrix voltages, Primitive64Matrix speeds) {
		return voltages.multiply(module * reduction * motorConstant / armatureResistance)
				.subtract(
						speeds.multiply(
								module
										* pow(reduction, 2)
										* pow(motorConstant, 2)
										/ armatureResistance))
//				.subtract(friction(speeds))
				;
	}

	/**
	 * Returns the power drawn by the motor
	 *
	 * @param voltage the voltage the motor is being supplied with
	 * @param speed the speed of the motor (rad/s)
	 * @return the power drawn by the motor (watts)
	 */
	public double power(double voltage, double speed) {
		return voltage * (voltage - reduction * motorConstant * speed) / armatureResistance;
	}

	/**
	 * Returns the power drawn by the motor (vectorised)
	 *
	 * @param voltages the voltages the motor is being supplied with
	 * @param speeds the speeds of the motor (rad/s)
	 * @return the power drawn by the motors (watts)
	 */
	public Primitive64Matrix power(Primitive64Matrix voltages, Primitive64Matrix speeds) {
		var result =
				(voltages.subtract(speeds.multiply(reduction * motorConstant))
						.divide(armatureResistance));
		var resultArray = new double[(int) result.size()];
		for (int i = 0; i < result.size(); i++) {
			resultArray[i] = result.get(i) * voltages.get(i);
		}

		return Primitive64Matrix.FACTORY.row(resultArray);
	}
}
