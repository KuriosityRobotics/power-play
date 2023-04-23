package com.kuriosityrobotics.powerplay.navigation;

import com.kuriosityrobotics.powerplay.math.MathUtil;
import com.kuriosityrobotics.powerplay.math.Pose;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public final class Junction{
    public final JunctionTypes type;
    public final Vector2D position;

    /**
     * @param jt the type of this junction
     * @param p the position of this junction in the odometry coordinate system
     */
    public Junction(JunctionTypes jt, Vector2D p){
        type = jt;
        position = p;
    }

	public String toString(){
		return "Junction of type " + type + " at coordinates (" + position.getX() + ", "  + position.getY() + ")";
	}

	public Pose[] pathFrom(Pose currentPosition){
		// we will only need 3 points: start, turn, end
		Pose[] points = new Pose[3];

		// round position to 24(n + 0.5) -> first position
		double x = currentPosition.x() / FieldNavConstants.GRID_SIZE;
		x -= x % 0.5;
		double y = currentPosition.y() / FieldNavConstants.GRID_SIZE;
		y -= y % 0.5;

		// corner cases (and it can still break if start and end are on the edges (more specifically the right edge)
		// of the field (I think)).
		if (x % 1 == 0) {
			if (position.getX() < currentPosition.x()) {
				x -= 0.5;
			} else {
				x += 0.5;
			}
		}
		if (y % 1 == 0) {
			if (position.getY() < currentPosition.y()) {
				y -= 0.5;
			} else {
				y += 0.5;
			}
		}

		points[0] = new Pose(
			x * FieldNavConstants.GRID_SIZE,
			y * FieldNavConstants.GRID_SIZE,
			position.getX() < x * FieldNavConstants.GRID_SIZE ? Math.PI : 0
		);


		// since the final destination is not in the middle of a grid square, we have to find the nearest one
		double orientation;
		Vector2D dest = position;

		// to left or to the right
		if (position.getX() < points[0].x()) {
			dest = dest.add(new Vector2D(FieldNavConstants.GRID_SIZE / 2, 0));
			orientation = Math.PI;
		} else {
			dest = dest.add(new Vector2D(-FieldNavConstants.GRID_SIZE / 2, 0));
			orientation = 0;
		}
		if (position.getY() < points[0].y()) {
			dest = dest.add(new Vector2D(0, FieldNavConstants.GRID_SIZE / 2));
			orientation = MathUtil.average(orientation, -Math.PI / 2);
		} else {
			dest = dest.add(new Vector2D(0, -FieldNavConstants.GRID_SIZE / 2));
			orientation = MathUtil.average(orientation, Math.PI / 2);
		}
		points[2] = new Pose(dest.getX(), dest.getY(), orientation);


		// second point is an in-between kinda thing so calculate it last
		points[1] = new Pose(
			points[2].x(),
			points[0].y(),
			points[0].orientation()
		);

		for (int i = 0; i < 3; i++){
			points[i] = new Pose(
				points[i].x() - FieldNavConstants.ROBOT_DIMENSIONS.getX(),
				points[i].y() - FieldNavConstants.ROBOT_DIMENSIONS.getY(),
				points[i].orientation()
			);
		}
		return points;
	}
}
