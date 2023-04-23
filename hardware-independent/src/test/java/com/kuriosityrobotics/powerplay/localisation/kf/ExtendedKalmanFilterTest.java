package com.kuriosityrobotics.powerplay.localisation.kf;

import static com.kuriosityrobotics.powerplay.math.MathUtil.diagonal;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.ojalgo.matrix.Primitive64Matrix;

class ExtendedKalmanFilterTest {
	@Test
	void testPrediction() {
		var kf = new ExtendedKalmanFilter(1, 1, 1);
		kf.builder().mean(1, 1, 1).variance(1, 1, 1).predict();

		var mean = kf.outputVector();
		assertArrayEquals(mean, new double[] {2, 2, 2});
		assertArrayEquals(kf.covariance().toRawCopy2D(), diagonal(1, 1, 1).toRawCopy2D());

		assertEquals(
				"Prediction data must be full-state.  Perhaps you could pass in 0 for the"
						+ " parameters you don't want to muck with.",
				assertThrows(
								IllegalArgumentException.class,
								() ->
										kf.builder()
												.mean(1, 1)
												.stateToOutput(
														Primitive64Matrix.FACTORY.rows(
																new double[][] {
																	{1, 0, 1},
																	{0, 1, 1}
																}))
												.variance(1, 1)
												.predict())
						.getMessage());
		assertEquals(
				"Mean must not be null.",
				assertThrows(
								IllegalArgumentException.class,
								() -> kf.builder().mean((Primitive64Matrix) null).predict())
						.getMessage());
		assertEquals(
				"Mean must not be null.",
				assertThrows(
								IllegalArgumentException.class,
								() -> kf.builder().variance(1, 1, 1).predict())
						.getMessage());

		assertEquals(
				"Covariance must not be null.",
				assertThrows(
								IllegalArgumentException.class,
								() -> kf.builder().mean(1, 1, 1).covariance(null).predict())
						.getMessage());
		assertEquals(
				"Covariance must not be null.",
				assertThrows(
								IllegalArgumentException.class,
								() -> kf.builder().mean(1, 1, 1).predict())
						.getMessage());
		assertEquals(
				"Covariance must not be null.",
				assertThrows(
								IllegalArgumentException.class,
								() ->
										kf.builder()
												.mean(1, 1, 1)
												.covariance((Primitive64Matrix) null)
												.predict())
						.getMessage());

		// nonsquare covariance matrix
		assertEquals(
				"Covariance must be square.",
				assertThrows(
								IllegalArgumentException.class,
								() ->
										kf.builder()
												.mean(1, 1)
												.covariance(
														Primitive64Matrix.FACTORY.rows(
																new double[][] {
																	{1, 0},
																}))
												.predict())
						.getMessage());
		// covariance matrix does not fit mean
		assertEquals(
				"Covariance does not fit mean.",
				assertThrows(
								IllegalArgumentException.class,
								() ->
										kf.builder()
												.mean(1, 1, 1)
												.covariance(
														Primitive64Matrix.FACTORY.rows(
																new double[][] {
																	{1, 0},
																	{0, 1}
																}))
												.predict())
						.getMessage());
		assertEquals(
				"State to output matrix does not fit filter.",
				assertThrows(
								IllegalArgumentException.class,
								() ->
										kf.builder()
												.mean(1, 1, 1)
												.variance(1, 1, 1)
												.stateToOutput(
														Primitive64Matrix.FACTORY.rows(
																new double[][] {
																	{1, 0},
																	{0, 1},
																	{0, 1}
																}))
												.predict())
						.getMessage());
		// symmetric covariance matrix
		assertEquals(
				assertThrows(
								IllegalArgumentException.class,
								() ->
										kf.builder()
												.mean(1, 1)
												.covariance(
														Primitive64Matrix.FACTORY.rows(
																new double[][] {
																	{1, 1},
																	{0, 1},
																}))
												.predict())
						.getMessage(),
				"Covariance matrix must be symmetrical.");
	}
}
