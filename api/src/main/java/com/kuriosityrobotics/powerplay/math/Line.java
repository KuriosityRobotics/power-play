package com.kuriosityrobotics.powerplay.math;

import java.util.ArrayList;

public class Line {
    private final Point startPoint;
    private final Point endPoint;

    //for pathfollow (LINE SEGMENT)
    public Line(Point startPoint, Point endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    //for finding intersections (LINE)
    public Line(Point point, double slope) {
        this(point, new Point(point.x() + 1, point.y() + slope));
    }

    public boolean isVertical() {
        return MathUtil.doublesEqual(startPoint.x(), endPoint.x());
    }

    public Point getIntersection(Line other) {
        if (getSlope() == other.getSlope()) {
            return null;
        }
        if (this.isVertical() && !other.isVertical()) {
            return new Point(startPoint.x(), startPoint.x() * other.getSlope() + other.getYInt());
        } else if (!this.isVertical() && other.isVertical()) {
            return new Point(other.startPoint.x(), other.startPoint.x() * getSlope() + getYInt());
        }
        //y = slope(x - startPoint.x) + startPoint.y
        //y = slope*x + (startPoint.y - slope*startPoint.x)
        double a = getSlope();
        double b = startPoint.y() - getSlope() * startPoint.x();
        double c = other.getSlope();
        double d = other.getYInt();

        double x = (d - b) / (a - c);
        double y = getSlope() * x + getYInt();

        return new Point(x, y);
    }

    public ArrayList<Point> pointsOnSegment(ArrayList<Point> points) {
        ArrayList<Point> onSegment = new ArrayList<>();

        for (Point point : points) {
            if (containsPointOnSegment(point)){
                onSegment.add(point);
            }
        }

        return onSegment;
    }

    public boolean containsPointOnSegment(Point point) {
        double minX = Math.min(startPoint.x(), endPoint.x());
        double maxX = Math.max(startPoint.x(), endPoint.x());
        double minY = Math.min(startPoint.y(), endPoint.y());
        double maxY = Math.max(startPoint.y(), endPoint.y());

        boolean withinX = point.x() >= minX && point.x() <= maxX;
        boolean withinY = point.y() >= minY && point.y() <= maxY;

        return withinX && withinY;
    }

    public Point closerToEnd(ArrayList<Point> points) {
        if (points.size() == 0){
            return new Point(0, 0);
        }
        double minDistance = Double.MAX_VALUE;
        Point closest = points.get(0);
        for (Point p : points) {
            if (p.distance(endPoint) < minDistance) {
                minDistance = p.distance(endPoint);
                closest = p;
            }
        }
        return closest;
    }

    public ArrayList<Point> pointsOnPathEnd(ArrayList<Point> points) {
        ArrayList<Point> onRay = new ArrayList<>();
        boolean withinX;
        boolean withinY;

        for (Point point : points) {
            if (endPoint.x() > startPoint.x()){
                withinX = point.x() > startPoint.x();
            }else{
                withinX = point.x() <= startPoint.x();
            }
            if (endPoint.y() > startPoint.y()){
                withinY = point.y() > startPoint.y();
            }else{
                withinY = point.y() <= startPoint.y();
            }

            if (withinX && withinY) {
                if (containsPointOnSegment(point)){
                    onRay.add(point);
                }else{
                    onRay.add(endPoint);
                }
            }
        }

        return onRay;
    }

	public double distanceToPoint(Point p) { // we have a y = mx + b line
		if (!isVertical()) {
			double sa = getSlope();
			double sb = -1;
			double sc = getYInt();

			return Math.abs(p.x() * sa + p.y() * sb + sc) / Math.hypot(getSlope(), 1);
		} else {
			return Math.abs(p.x() - startPoint.x()); // start and end should have the same x value so it should not matter
		}
	}

    public double getSlope() {
        if (isVertical()) {
            return Double.MAX_VALUE;
        }
        return (endPoint.y() - startPoint.y()) / (endPoint.x() - startPoint.x());
    }

    public Point getStartPoint(){
        return startPoint;
    }

    public Point getEndPoint(){
        return endPoint;
    }

    public double getYInt() {
        return startPoint.y() - getSlope() * startPoint.x();
    }

	public boolean containsPoint(Point point) {
		double minX = Math.min(startPoint.x(), endPoint.x());
		double maxX = Math.max(startPoint.x(), endPoint.x());
		double minY = Math.min(startPoint.y(), endPoint.y());
		double maxY = Math.max(startPoint.y(), endPoint.y());

		boolean withinX = point.x() >= minX && point.x() <= maxX;
		boolean withinY = point.y() >= minY && point.y() <= maxY;

		return withinX && withinY;
	}
}
