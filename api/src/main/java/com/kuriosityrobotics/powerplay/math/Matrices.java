package com.kuriosityrobotics.powerplay.math;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import org.ojalgo.matrix.Primitive64Matrix;

public class Matrices {
	/**
	 * @param items the items to be placed in the diagonal matrix
	 * @return a diagonal matrix with the given items on the diagonal
	 */
	public static Primitive64Matrix diag(double... items) {
		var array = new double[items.length][items.length];
		for (int i = 0; i < items.length; i++) {
			array[i][i] = items[i];
		}

		return Primitive64Matrix.FACTORY.rows(array);
	}

	/**
	 * @param theta the angle of rotation in radians (clockwise)
	 * @return the rotation matrix for the given angle
	 */
	public static Primitive64Matrix rotationMatrix(double theta) {
		return Primitive64Matrix.FACTORY.rows(
				new double[][] {
					{cos(theta), -sin(theta), 0},
					{sin(theta), cos(theta), 0},
					{0, 0, 1}
				});
	}
}
