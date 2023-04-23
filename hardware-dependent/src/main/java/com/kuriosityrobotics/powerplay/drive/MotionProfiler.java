package com.kuriosityrobotics.powerplay.drive;

import android.util.Log;

import com.acmerobotics.dashboard.config.Config;
import com.kuriosityrobotics.powerplay.math.Circle;
import com.kuriosityrobotics.powerplay.math.Line;
import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;

import java.util.List;

@Config
public class MotionProfiler {
	public static double MAX_VEL = 50;
	public static double MAX_ACCEL = 45;
	public static double MAX_DECCEL = 65;

	private final Point start;
	private final Point end;
	private final double totalDistance;
	private final double startVelo;
	private double maxVel = MAX_VEL;

	// distances
	private double accelThreshold;
	private double deccelThreshold;

	public MotionProfiler(Pose start, Pose end, double startVeloMag) {
		this.start = start;
		this.end = end;
		this.totalDistance = start.distance(end);
		this.startVelo = Math.max(startVeloMag, MAX_VEL);

		findThresholds();
	}

	private void findThresholds() {
		Line accel = new Line(new Point(0, startVelo),
			new Point((MAX_VEL * MAX_VEL - startVelo * startVelo) / (2 * MAX_ACCEL), MAX_VEL));
		Line deccel = new Line(new Point(totalDistance - (MAX_VEL * MAX_VEL) / (2 * MAX_DECCEL), MAX_VEL),
			new Point(totalDistance, 0));

		Point tmp = accel.getIntersection(deccel);

		Log.v("MP", "intersection point: " + tmp.toString());

		if (tmp.getX() > 0) {
			if (tmp.getY() >= MAX_VEL) {
				this.accelThreshold = accel.getIntersection(new Line(new Point(0, MAX_VEL), new Point(1, MAX_VEL))).getX();
				this.deccelThreshold = deccel.getIntersection(new Line(new Point(0, MAX_VEL), new Point(1, MAX_VEL))).getX();

				this.maxVel = MAX_VEL;
			} else {
				this.accelThreshold = tmp.getX();
				this.deccelThreshold = totalDistance - tmp.getX();
				this.maxVel = tmp.getY();
			}
		} else {
			this.maxVel = startVelo;
			this.accelThreshold = 0;
			this.deccelThreshold = totalDistance;
		}

		Log.v("MP", "MAX VEL: " + maxVel);
		Log.v("MP", "accel threshold: " + accelThreshold);
		Log.v("MP", "deccel threshold: " + deccelThreshold);
	}

	public double getTargetVeloMag(Pose currentPosition) {
		Point newPos = clipToPath(currentPosition);

		double distToStart = newPos.distance(start);
		double distToEnd = newPos.distance(end);

		Log.v("MP", "dist to start: " + distToStart);
		Log.v("MP", "dist to end: " + distToEnd);

		double result = maxVel;

		if (distToStart < accelThreshold) { // up on the trapezoidal profile
			result = Math.min((maxVel * distToStart / accelThreshold) + 11, maxVel);
		} else if (distToEnd < deccelThreshold) { // down the triangular profile
			result = Math.min((maxVel * distToEnd / deccelThreshold), maxVel);
		}

		Log.v("MP", "result: " + result);
		return Math.min(maxVel, result);
	}

	private Point clipToPath(Point robotPosition) {
		Point nearestClippedPoint = start; // default clip to start of path?

		// clip the robot's position onto the segment.
		Line segment = new Line(start, end);

		Point clipped = robotPosition.projectToSegment(segment);
		if (segment.containsPoint(clipped)) nearestClippedPoint = clipped;

		// return the clipped point
		return nearestClippedPoint;
	}
}
