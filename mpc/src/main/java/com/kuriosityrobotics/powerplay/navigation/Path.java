package com.kuriosityrobotics.powerplay.navigation;

import static java.lang.Math.pow;

import com.kuriosityrobotics.powerplay.math.Point;
import com.kuriosityrobotics.powerplay.math.Pose;
import org.apache.commons.csv.CSVFormat;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Path {
	List<Point> path = new ArrayList<>();

	public Path(List<Point> path) {
		this.path = List.copyOf(path);
	}

	public Path(Point... path) {
		this.path = List.of(path);
	}

	public static Path fromCSV(InputStream reader) throws IOException {
		var csv = CSVFormat.DEFAULT.withFirstRecordAsHeader().withRecordSeparator('\n').parse(new InputStreamReader(reader)).getRecords();
		var points = csv.stream().map(
			record -> new Point(
				Double.parseDouble(record.get("x_desired")),
				Double.parseDouble(record.get("y_desired"))
			)
		).collect(Collectors.toList());

		return new Path(points);
	}

	public double pathLength() {
		double totalLength = 0;
		for (int i = 0; i < (path.size() - 1); i++) {
			totalLength += path.get(i).distance(path.get(i + 1));
		}
		return totalLength;
	}

	public double partialPathLength(int points) {
		double partialLength = 0;
		for (int i = 0; i < (points - 1); i++) {
			partialLength += path.get(i).distance(path.get(i + 1));
		}
		return partialLength;
	}

	public Point distanceAlong(double distance) {
		if (distance <= 0) {
			return path.get(0);
		}
		if (distance >= pathLength()) {
			return path.get(path.size() - 1);
		}

		double fullSegmentDistance = 0;
		int targetSegmentStart = 0;

		for (int i = 0; i < path.size() - 1; i++) {
			var segmentLength = path.get(i).distance(path.get(i + 1));
			fullSegmentDistance += segmentLength;

			if (fullSegmentDistance >= distance) {
				targetSegmentStart = i;
				break;
			}
		}

		var segmentRatio = (fullSegmentDistance - distance) / path.get(targetSegmentStart).distance(path.get(targetSegmentStart + 1));
		var result = path.get(targetSegmentStart).scalarMutliply(segmentRatio).add(path.get(targetSegmentStart + 1).scalarMutliply(1. - segmentRatio));
		return result;
	}

	public Point[] distancesAlong(double[] distances) {
		Point[] points = new Point[distances.length];
		for (int i = 0; i < distances.length; i++) {
			points[i] = distanceAlong(distances[i]);
		}
		return points;
	}

	public Point get(int i) {
		return path.get(i);
	}

	public int size() {
		return path.size();
	}

	// returns the distance traversed along the path of the nearest waypoint to the input point
	public double closestWaypointLength(Point point) {
		int minIndex = 0;
		double minDistance = point.distance(get(0));
		for (int i = 0; i < path.size(); i++) {
			double distance = point.distance(get(i));
			if (distance < minDistance) {
				minIndex = i;
				minDistance = distance;
			}
		}
		return partialPathLength(minIndex + 1); // index+1 to convert to number of points
	}

	private static double INTERPOLATION_STEP = 0.01;

	// returns the distance traversed to the closest interpolated point on the path
	public double closestPointLength(Point point) {
		// check endpoint explicitly first to ensure it is factored in, even if not multiple of INTERPOLATION_STEP
		double minLength = pathLength();
		double minDistance = point.distance(distanceAlong(minLength));

		for (double length = 0.; length < pathLength(); length += INTERPOLATION_STEP) {
			double distance = point.distance(distanceAlong(length));
			if (distance < minDistance) {
				minLength = length;
				minDistance = distance;
			}
		}
		if (minLength > pathLength()) {
			minLength = pathLength();
		}

		return minLength;
	}

	public double closestPointLengthInRange(Point point, double minRange, double maxRange) {
		if (minRange < 0.) {
			minRange = 0.;
		}

		if (maxRange > pathLength()) {
			maxRange = pathLength();
		}

		// check endpoint explicitly
		double minLength = maxRange;
		double minDistance = point.distance(distanceAlong(minLength));

		for (double length = minRange; length < maxRange; length += INTERPOLATION_STEP) {
			double distance = point.distance(distanceAlong(length));
			if (distance < minDistance) {
				minLength = length;
				minDistance = distance;
			}
		}
		if (minLength > pathLength()) {
			minLength = pathLength();
		}

		return minLength;
	}

	public double curvature(double length) {
		if (length - INTERPOLATION_STEP < 0) {
			return 0;
		}
		if (length + INTERPOLATION_STEP > pathLength()) {
			return 0;
		}

		// curvature = sqrt(y''(l)^2 + x''(l)^2)
		Point secondDerivatives = 
			distanceAlong(length + INTERPOLATION_STEP)
			.add(distanceAlong(length - INTERPOLATION_STEP))
			.add(distanceAlong(length).scalarMutliply(-2.))
			.scalarMutliply(pow(INTERPOLATION_STEP, -2.));
		
		double curvature = secondDerivatives.distance(Point.zero()); // silly hack to get magnitude of the secondderivatives vector
		if (curvature > 5) {
			return 5;
		}

		return curvature;
	}

	public Path inverse() {
		Point[] points = new Point[path.size()];
		for (int i = 0; i < path.size(); i++) {
			points[i] = path.get(path.size() - i - 1);
		}
		return new Path(points);
	}

	public Path toFTCSystem() {
		Point[] points = new Point[path.size()];
		for (int i = 0; i < path.size(); i++) {
			if (path.get(i) instanceof Pose) points[i] = ((Pose) path.get(i)).toFTCSystem();
			else points[i] = path.get(i).toFTCSystem();
		}

		return new Path(points);
	}

	public Path fromStartingPoint(Point point) {
		// returns this path but with the starting point at the beginning of the path
		Point[] points = new Point[path.size() + 1];
		points[0] = point;
		for (int i = 0; i < path.size(); i++) {
			points[i + 1] = path.get(i);
		}
		return new Path(points);
	}

	public Path fromStartingPoints(Point... points){
		Point[] newPoints = new Point[path.size() + points.length];
		System.arraycopy(points, 0, newPoints, 0, points.length);
		for (int i = 0; i < path.size(); i++) {
			newPoints[i + points.length] = path.get(i);
		}

		return new Path(newPoints);
	}
}
