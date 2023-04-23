package com.kuriosityrobotics.powerplay.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OneDimentionalPathTest {
	@Test
	public void testPathConstruction() {
		Double[] wayPoints = new Double[] {
			0.,
			-1.,
			1.,
			2.,
			0.
		};

		OneDimentionalPath path = new OneDimentionalPath(wayPoints);
	}

	@Test
	public void testPathLength() {
		Double[] wayPoints = new Double[] {
			0.,
			-1.,
			1.,
			2.,
			0.
		};

		OneDimentionalPath path = new OneDimentionalPath(wayPoints);

		assertEquals(6., path.pathLength());
	}

	@Test
	public void testDistanceAlong() {
		Double[] wayPoints = new Double[] {
			0.,
			-1.,
			1.,
			2.,
			0.
		};

		OneDimentionalPath path = new OneDimentionalPath(wayPoints);

		assertEquals(0., path.distanceAlong(-5.)); // before the start; returns the start position
		assertEquals(0, path.distanceAlong(0.));
		assertEquals(-0.5, path.distanceAlong(0.5));
		assertEquals(-1., path.distanceAlong(1.));
		assertEquals(-0.5, path.distanceAlong(1.5));
		assertEquals(0., path.distanceAlong(2.));
		assertEquals(0.5, path.distanceAlong(2.5));
		assertEquals(1., path.distanceAlong(3.));
		assertEquals(1.5, path.distanceAlong(3.5));
		assertEquals(2., path.distanceAlong(4.));
		assertEquals(1.5, path.distanceAlong(4.5));
		assertEquals(1., path.distanceAlong(5.));
		assertEquals(0.5, path.distanceAlong(5.5));
		assertEquals(0., path.distanceAlong(6.));
		assertEquals(0., path.distanceAlong(6. + 5.)); // after the end; returns the end position
	}

	@Test
	public void testDistancesAlong(){
		Double[] wayPoints = new Double[] {
			0.,
			-1.,
			1.,
			2.,
			0.
		};

		OneDimentionalPath path = new OneDimentionalPath(wayPoints);

		var points = new double[]{0.5, 1.5, 2.5, 3.5, 4.5, 5.5};
		var interpolatedPoints = new double[] {-0.5, -0.5, 0.5, 1.5, 1.5, 0.5};
		var computedPoints = path.distancesAlong(points);

		for (int i = 0; i < points.length; i++) {
			assertEquals(interpolatedPoints[i], computedPoints[i]);
		}
	}
}
