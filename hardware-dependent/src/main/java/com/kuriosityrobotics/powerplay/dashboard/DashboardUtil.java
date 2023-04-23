package com.kuriosityrobotics.powerplay.dashboard;

import com.acmerobotics.dashboard.canvas.Canvas;
import com.kuriosityrobotics.powerplay.navigation.Path;
import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;

import java.util.List;

/**
 * Set of helper functions for drawing Kuriosity robot and location history on dashboard canvases.
 */

public class DashboardUtil {
	private static final double ROBOT_RADIUS = 13.0 * Math.sqrt(2) / 2.0;

	public static void drawPoseHistory(Canvas canvas, List<Point> poseHistory) {
		canvas.setStrokeWidth(1);
		canvas.setStroke("black");
		double[] xPoints = new double[poseHistory.size()];
		double[] yPoints = new double[poseHistory.size()];
		for (int i = 0; i < poseHistory.size(); i++) {
			Point point = poseHistory.get(i);
			xPoints[i] = point.getX();
			yPoints[i] = point.getY();
		}
		canvas.strokePolyline(xPoints, yPoints);
	}

	public static void drawRobot(Canvas canvas, Pose pose) {
		canvas.setStrokeWidth(1);
		canvas.setStroke("red");

		canvas.strokeCircle(pose.getX(), pose.getY(), ROBOT_RADIUS);
		Point headingVector = new Point(Math.cos(pose.orientation()), Math.sin(pose.orientation())).scalarMutliply(ROBOT_RADIUS);
		double x1 = pose.getX() + headingVector.getX() / 2, y1 = pose.getY() + headingVector.getY() / 2;
		double x2 = pose.getX() + headingVector.getX(), y2 = pose.getY() + headingVector.getY();
		canvas.strokeLine(x1, y1, x2, y2);
	}

	public static void drawPath(Canvas canvas, Path path) {
		double[] xPoints = new double[path.size()];
		double[] yPoints = new double[path.size()];

		for (int i = 0; i < path.size(); i++) {
			xPoints[i] = path.get(i).x(); yPoints[i] = path.get(i).y();
		}

		canvas.setStroke("blue");
		canvas.strokePolyline(xPoints, yPoints);
	}
}