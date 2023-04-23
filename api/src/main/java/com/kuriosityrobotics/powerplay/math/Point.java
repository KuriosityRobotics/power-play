package com.kuriosityrobotics.powerplay.math;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.io.Serializable;

public class Point extends Vector2D implements Serializable {
    public Point(double x, double y) {
        super(x, y);
    }

	public static Point ofTileNumber(int x, int y) {
		// returns the centre of the tile; 0, 0 is the corner tile
		return new Point(0.3048 * (2 * x + 1), 0.3048 * (2 * y + 1));
	}

    public double distance(Point other) {
        return Math.hypot(other.x() - x(), other.y() - y());
    }

    public Point projectToLine(Line line) {
        Point nearest;
        if (line.isVertical()) {
            nearest = new Point(line.getStartPoint().x(), y());
        } else if (line.getSlope() == 0) {
            nearest = new Point(x(), line.getStartPoint().y());
        } else {
            Line perpendicular = new Line(this, -1 / line.getSlope());
            nearest = line.getIntersection(perpendicular);
        }

        return nearest;
    }

    //SEGMENTS
    public Point projectToSegment(Line line) {
        Point nearest = projectToLine(line);

        if (line.containsPointOnSegment(nearest)) {
            return nearest;
        } else {
            if (nearest.distance(line.getStartPoint()) <= nearest.distance(line.getEndPoint())) {
                return line.getStartPoint();
            }
            return line.getEndPoint();
        }
    }

    //SEGMENTS
    public double segmentDistance(Line line) {
        return this.distance(projectToSegment(line));
    }
    public boolean equals(Object o) {
        if (!(o instanceof Point)) {
            return false;
        }

        Point point = (Point) o;
        return MathUtil.doublesEqual(point.x(), x()) && MathUtil.doublesEqual(point.y(), y());
    }

	public double relativeHeadingTo(Point other){
		double dX = other.x() - this.x();
		double dY = other.y() - this.y();
		return Math.atan2(dY, dX);
	}

	public Point scalarMutliply(double scalar) {
		return new Point(x() * scalar, y() * scalar);
	}

    public double x() {
        return getX();
    }

    public double y() {
        return getY();
    }

	public Point toFTCSystem() {
		double x = -y() + (144. / 2.);
		double y = x() - (144. / 2.);
		return new Point(x, y);
	}

	public Point subtract(Point other) {
		return new Point(this.x() - other.x(), this.y() - other.y());
	}
	public Point add(Point other) {
		return new Point(this.x() + other.x(), this.y() + other.y());
	}

    public static Point zero() {
        return new Point(0, 0);
    }

    @Override
    public String toString(){
        return "x: " + x() + " y: " + y();
    }

	public double atan() {
		return Math.atan2(y(), x());
	}
}
