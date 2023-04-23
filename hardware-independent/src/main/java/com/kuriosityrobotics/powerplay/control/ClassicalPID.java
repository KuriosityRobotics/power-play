package com.kuriosityrobotics.powerplay.control;

import com.google.common.util.concurrent.AtomicDouble;
import com.kuriosityrobotics.powerplay.util.Instant;

public class ClassicalPID {
	public double P_FACTOR;
	public double I_FACTOR;
	public double D_FACTOR;

	private boolean reset;

	private volatile double lastError;
	private final AtomicDouble errorSum = new AtomicDouble();

	private Instant prevTime;

	/**
	 * Constructs a ClassicalPIDController
	 *
	 * @param p
	 * @param i
	 * @param d
	 */
	public ClassicalPID(double p, double i, double d) {
		P_FACTOR = p;
		I_FACTOR = i;
		D_FACTOR = d;
	}

	/**
	 * Update the PID controller's scale. Should be called every iteration. Accumulates error onto
	 * the integral.
	 *
	 * @param error The error between the current state and the desired state
	 * @return Updated speed
	 */
	public double calculateSpeed(double error) {
		var currentTime = Instant.now();

		double p = error * P_FACTOR;
		double i = errorSum.doubleValue() * I_FACTOR;
		double d = 0;

		if (prevTime != null) {
			var deltaTime = currentTime.since(prevTime);
			if (Double.isNaN(error / deltaTime.toSeconds()))
				throw new AssertionError();

			errorSum.addAndGet(error / deltaTime.toSeconds());
			var errorDerivative = (error - lastError) / deltaTime.toSeconds();
			d = D_FACTOR * errorDerivative;
		}

		lastError = error;
		prevTime = currentTime;

		if (Double.isNaN(p))
			throw new AssertionError();
		if (Double.isNaN(i))
			throw new AssertionError();
		if (Double.isNaN(d))
			throw new AssertionError();


		return p + i + d;
	}

	/**
	 * Reset the PID controller using given default scale
	 */

	public void reset() {
		errorSum.set(0);
		prevTime = null;
	}

	public void setConstants(double P, double I, double D) {
		this.P_FACTOR = P;
		this.I_FACTOR = I;
		this.D_FACTOR = D;
	}
}