package com.kuriosityrobotics.powerplay.math;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.ojalgo.matrix.Primitive64Matrix;

import java.io.Serializable;

/** This expresses velocity in free space with uncertainty. */
public class TwistWithCovariance extends Twist implements Serializable {
	private final transient Primitive64Matrix covariance;

	/**
	 * @param x inches per second
	 * @param y inches per second
	 * @param angular theta radians per second
	 * @param covariance covariance of data
	 */
	public TwistWithCovariance(double x, double y, double angular, Primitive64Matrix covariance) {
		super(x, y, angular);
		this.covariance = covariance;
	}

	public static TwistWithCovariance of(Twist twist, Primitive64Matrix covariance) {
		if (covariance.getColDim() != 3 || covariance.getRowDim() != 3)
			throw new IllegalArgumentException("Twist covariance must be 3x3");

		return new TwistWithCovariance(twist.x(), twist.y(), twist.angular(), covariance);
	}

	public Primitive64Matrix covariance() {
		return covariance;
	}

	public double[] getData() {
		return new double[] {x(), y(), angular()};
	}
}
