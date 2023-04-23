package com.kuriosityrobotics.powerplay.localisation.kf;

import org.ojalgo.matrix.Primitive64Matrix;

import java.util.Objects;

final class KalmanDatum {
	private final long time;
	private final Primitive64Matrix mean;
	private final Primitive64Matrix covariance;
	private final Primitive64Matrix stateToOutput;

	/**
	 * @param stateToOutput For a prediction, this is the inverse of matrix G (for example, undoes
	 *     rotation applied to convert odometry to global) For a correction, this is matrix H
	 */
	KalmanDatum(
			long time,
			Primitive64Matrix mean,
			Primitive64Matrix covariance,
			Primitive64Matrix stateToOutput) {
		this.time = time;
		this.mean = mean;
		this.covariance = covariance;
		this.stateToOutput = stateToOutput;
	}

	public long time() {
		return time;
	}

	public Primitive64Matrix mean() {
		return mean;
	}

	public Primitive64Matrix covariance() {
		return covariance;
	}

	public Primitive64Matrix stateToOutput() {
		return stateToOutput;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (KalmanDatum) obj;
		return this.time == that.time
				&& Objects.equals(this.mean, that.mean)
				&& Objects.equals(this.covariance, that.covariance)
				&& Objects.equals(this.stateToOutput, that.stateToOutput);
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, mean, covariance, stateToOutput);
	}

	@Override
	public String toString() {
		return "KalmanDatum["
				+ time
				+ ", "
				+ "mean="
				+ mean
				+ ", "
				+ "covariance="
				+ covariance
				+ ", "
				+ "stateToOutput="
				+ stateToOutput
				+ ']';
	}

	public Primitive64Matrix outputToState() {
		return stateToOutput.invert();
	}

	public Primitive64Matrix getStateToOutput() {
		return stateToOutput;
	}

	public boolean isFullState() {
		return stateToOutput.isSquare();
	}
}
