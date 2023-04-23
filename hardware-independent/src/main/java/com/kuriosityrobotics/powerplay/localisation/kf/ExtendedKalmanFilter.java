package com.kuriosityrobotics.powerplay.localisation.kf;

import com.kuriosityrobotics.powerplay.math.MathUtil;

import org.ojalgo.matrix.Primitive64Matrix;

// TODO:  documentation for this class
class ExtendedKalmanFilter {
	private Primitive64Matrix mean;
	private Primitive64Matrix covariance;

	/**
	 * @param initialState starting state
	 */
	public ExtendedKalmanFilter(double... initialState) {
		reset(initialState);
	}

	public ExtendedKalmanFilter(double[] initialState, double... initialVariance) {
		reset(initialState, initialVariance);
	}

	public static Primitive64Matrix propagateError(
			Primitive64Matrix function, Primitive64Matrix startingCovariance) {
		return function.multiply(startingCovariance).multiply(function.transpose());
	}

	public KalmanDatumBuilder builder() {
		return new KalmanDatumBuilder();
	}

	public synchronized void reset(double[] initialState, double... initialVariance) {
		mean = Primitive64Matrix.FACTORY.column(initialState);
		covariance = MathUtil.diagonal(initialVariance);
	}

	public synchronized void reset(double... initialState) {
		reset(initialState, new double[initialState.length]);
	}

	private synchronized void predict(KalmanDatum datum) {
		mean = mean.add(datum.outputToState().multiply(datum.mean()));
		covariance = covariance.add(propagateError(datum.outputToState(), datum.covariance()));
	}

	private synchronized void correct(KalmanDatum datum) {
		var W = stateCorrectionCovariance(datum.getStateToOutput(), datum.covariance());

		var innovation = innovation(datum.getStateToOutput(), datum.mean());

		mean = mean.add(W.multiply(innovation));
		covariance =
				covariance.subtract(
						propagateError(W, propagateError(datum.getStateToOutput(), covariance)));
	}

	public Primitive64Matrix covariance() {
		return covariance;
	}

	public double[] outputVector() {
		return mean.columns().next().toRawCopy1D();
	}

	private Primitive64Matrix stateCorrectionCovariance(
			Primitive64Matrix stateToOutput, Primitive64Matrix outputCovariance) {
		assert outputCovariance.isSquare() && outputCovariance.getDeterminant() != 0;
		return covariance
				.multiply(stateToOutput.transpose())
				.multiply(propagateError(stateToOutput, covariance).add(outputCovariance).invert());
	}

	private Primitive64Matrix innovation(
			Primitive64Matrix stateToOutput, Primitive64Matrix output) {
		return output.subtract(stateToOutput.multiply(mean));
	}

	@SuppressWarnings("unused")
	public class KalmanDatumBuilder {
		private Long time = System.currentTimeMillis();
		private Primitive64Matrix mean, covariance, stateToOutput;

		public KalmanDatumBuilder mean(double... mean) {
			this.mean = Primitive64Matrix.FACTORY.column(mean);
			return this;
		}

		public KalmanDatumBuilder mean(Primitive64Matrix mean) {
			this.mean = mean;
			return this;
		}

		public KalmanDatumBuilder variance(double... variance) {
			this.covariance = MathUtil.diagonal(variance);
			return this;
		}

		public KalmanDatumBuilder covariance(Primitive64Matrix covariance) {
			this.covariance = covariance;
			return this;
		}

		public KalmanDatumBuilder stateToOutput(Primitive64Matrix stateToOutput) {
			this.stateToOutput = stateToOutput;
			return this;
		}

		public KalmanDatumBuilder outputToState(Primitive64Matrix outputToState) {
			this.stateToOutput = outputToState.invert();
			return this;
		}

		public KalmanDatumBuilder time(long time) {
			this.time = time;
			return this;
		}

		private KalmanDatum build() {
			if (mean == null) throw new IllegalArgumentException("Mean must not be null.");
			if (covariance == null)
				throw new IllegalArgumentException("Covariance must not be null.");
			if (stateToOutput == null)
				stateToOutput = Primitive64Matrix.FACTORY.makeIdentity(mean.getRowDim());

			if (!covariance.isSquare())
				throw new IllegalArgumentException("Covariance must be square.");

			if (covariance.getRowDim() != mean.getRowDim())
				throw new IllegalArgumentException("Covariance does not fit mean.");

			if (!covariance.isSymmetric())
				throw new IllegalArgumentException("Covariance matrix must be symmetrical.");

			if (stateToOutput.getColDim() != ExtendedKalmanFilter.this.mean.getRowDim())
				throw new IllegalArgumentException("State to output matrix does not fit filter.");

			return new KalmanDatum(time, mean, covariance, stateToOutput);
		}

		public void predict() {
			var datum = build();
			if (!(datum.isFullState()
					&& this.mean.getRowDim() == ExtendedKalmanFilter.this.mean.getRowDim()))
				throw new IllegalArgumentException(
						"Prediction data must be full-state.  Perhaps you could pass in 0 for the"
								+ " parameters you don't want to muck with.");

			ExtendedKalmanFilter.this.predict(datum);
		}

		public void correct() {
			var datum = build();
			ExtendedKalmanFilter.this.correct(datum);
		}
	}
}
