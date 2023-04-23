package com.kuriosityrobotics.powerplay.navigation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OneDimentionalPath {
	private final List<Double> path = new ArrayList<>();

	public OneDimentionalPath(Double[] wayPoints){
		path.addAll(Arrays.asList(wayPoints));
	}

	public double pathLength() {
		double totalLength = 0;
		for (int i = 0; i < (path.size() - 1); i++) {
			totalLength += Math.abs(path.get(i) - path.get(i+1));
		}
		return totalLength;
	}

	public double distanceAlong(double distance) {
		if (distance <= 0) {
			return path.get(0);
		}
		if (distance >= pathLength()) {
			return path.get(path.size() - 1);
		}

		double fullSegmentDistance = 0;
		int targetSegmentStart = 0;

		for (int i = 0; i < path.size() - 1; i++) {
			var segmentLength = Math.abs(path.get(i) - path.get(i + 1));
			fullSegmentDistance += segmentLength;

			if (fullSegmentDistance >= distance){
				targetSegmentStart = i;
				break;
			}
		}

		var segmentRatio = (fullSegmentDistance - distance) / Math.abs(path.get(targetSegmentStart) - path.get(targetSegmentStart + 1));
		var result = path.get(targetSegmentStart) * segmentRatio + path.get(targetSegmentStart+1) * (1. - segmentRatio);
		return result;
	}

	public double[] distancesAlong(double[] distances) {
		double[] points = new double[distances.length];
		for (int i = 0; i < distances.length; i++) {
			points[i] = distanceAlong(distances[i]);
		}
		return points;
	}

	public double get(int i){
		return path.get(i);
	}

	public int size(){
		return path.size();
	}

}
