package com.kuriosityrobotics.powerplay.localisation;

import org.ojalgo.matrix.Primitive64Matrix;

public class LocalisationUtil {
	/**
	 * Returns a covariance matrix of a vector random variable given the standard deviations of its
	 * components
	 *
	 * @param stdev the standard deviation of each random variable
	 * @return a diagonal matrix of the standard deviations squared
	 */
	public static Primitive64Matrix covarianceFromStandardDeviation(double... stdev) {
		for (int i = 0; i < stdev.length; i++) stdev[i] = stdev[i] * stdev[i]; // variance = stdev^2

		var resultArray = new double[stdev.length][stdev.length];
		for (int i = 0; i < stdev.length; i++) {
			resultArray[i][i] = stdev[i];
		}

		return Primitive64Matrix.FACTORY.rows(resultArray);
	}
}
