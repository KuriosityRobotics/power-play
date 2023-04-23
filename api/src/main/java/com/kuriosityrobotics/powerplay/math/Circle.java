package com.kuriosityrobotics.powerplay.math;

import java.util.ArrayList;

public class Circle {
    private final Point center;
    private final double radius;

    public Circle(Point center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    public double centerDistance(Line line) {
        double distance;
        if (line.isVertical()) {
            distance = Math.abs(center.x() - line.getStartPoint().x());
        }else if (line.getSlope() == 0) {
            distance = Math.abs(center.y() - line.getStartPoint().y());
        }else {
            Line perpendicular = new Line(center, -1 / line.getSlope());
            Point intersection = line.getIntersection(perpendicular);
            distance = center.distance(intersection);
        }
        return distance;
    }

    public boolean intersects(Line line) {
        return centerDistance(line) <= radius;
    }

    public ArrayList<Point> getIntersections(Line line) {
        ArrayList<Point> intersections = new ArrayList<>();

        // circle equation is (x-center.x)^2 + (y-center.y)^2 = radius^2
        if (!this.intersects(line)) {
            return intersections;
        } else if (line.isVertical()) { // line equation is x=startPoint.x
            double sqrtExpression = Math.sqrt(radius*radius - Math.pow(line.getStartPoint().x() - center.x(), 2));

            intersections.add(new Point(line.getStartPoint().x(), center.y() + sqrtExpression));
            if (sqrtExpression != 0) {
                intersections.add(new Point(line.getStartPoint().x(), center.y() - sqrtExpression));
            }
        } else if (line.getSlope() == 0) { // line equation is y = startPoint.y
            double sqrtExpression = Math.sqrt(radius*radius - Math.pow(line.getStartPoint().y() - center.y(), 2));

            intersections.add(new Point(center.x() + sqrtExpression, line.getStartPoint().y()));
            if (sqrtExpression != 0) {
                intersections.add(new Point(center.x() - sqrtExpression, line.getStartPoint().y()));
            }
        } else { // line equation is y = slope*x + yInt
            double a = line.getSlope() * line.getSlope() + 1;
            double b = 2 * line.getYInt() * line.getSlope() - 2 * center.y() * line.getSlope() - 2 * center.x();
            double c = Math.pow(line.getYInt() - center.y(), 2) + center.x()*center.x() - radius*radius;

            double[] xValues = quadraticFormula(a, b, c);
            intersections.add(new Point(xValues[0], xValues[0]* line.getSlope() + line.getYInt()));
            if (xValues[0] != xValues[1]) {
                intersections.add(new Point(xValues[1], xValues[1]* line.getSlope() + line.getYInt()));
            }
        }
        return intersections;
    }

    public ArrayList<Point> getSegmentIntersections(Line line){
        ArrayList<Point> intersections = getIntersections(line);
        return line.pointsOnSegment(intersections);
    }

    public ArrayList<Point> getPathEndIntersections(Line line){
        ArrayList<Point> intersections = getIntersections(line);
        return line.pointsOnPathEnd(intersections);
    }

    public static double[] quadraticFormula(double a, double b, double c) {
        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            return new double[0];
        }
        return new double[] { (-b + Math.sqrt(discriminant)) / (2 * a), (-b - Math.sqrt(discriminant)) / (2 * a) };
    }
}
