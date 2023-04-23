package com.kuriosityrobotics.powerplay.navigation;

import com.kuriosityrobotics.powerplay.math.MathUtil;
import com.kuriosityrobotics.powerplay.math.Pose;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import static com.kuriosityrobotics.powerplay.math.MathUtil.clamp;

public class FieldNavigator {
   private static Junction getJunctionAtGrid(int x, int y) {
	  return new Junction(
			  FieldNavConstants.field[x][y],
			  new Vector2D(
					  (x + 1) * FieldNavConstants.GRID_SIZE,
					  (y + 1) * FieldNavConstants.GRID_SIZE
			  )
	  );
   }

   private static int[] getNearestGridPoint(Pose p) {
	  // adding 0.5 to avoid Math.round() which returns long
	  Vector2D poseInGrid = new Vector2D(
			  (int) (p.x() / FieldNavConstants.GRID_SIZE + 0.5),
			  (int) (p.y() / FieldNavConstants.GRID_SIZE + 0.5)
	  );

	  // because edges
	  return new int[]{
			  clamp((int) poseInGrid.getX(), 1, FieldNavConstants.field.length) - 1,
			  clamp((int) poseInGrid.getY(), 1, FieldNavConstants.field.length) - 1,
	  };
   }

   /**
	* @param p the current position of the robot
	* @return the nearest junction (wow).
	*/
   public static Junction getNearestJunction(Pose p) {
	  // very gp
	  int[] gp = getNearestGridPoint(p);
	  return getJunctionAtGrid(gp[0], gp[1]);
   }

   /**
	* @param p    the current position of the robot
	* @param type the type of junction you wish to find (see {@link JunctionTypes})
	* @return the nearest junction of type {@code type}.
	*/
   public static Junction getNearestJunctionOfType(Pose p, JunctionTypes type) {
	  double minDist = 0;
	  Junction best = null;
	  for (int i = 0; i < FieldNavConstants.field.length; i++) {
		 for (int j = 0; j < FieldNavConstants.field[0].length; j++) {
			Junction current = getJunctionAtGrid(i, j);
			if (current.type == type) {
			   double dist = p.distanceSq(current.position);
			   if (best == null || dist < minDist) {
				  best = current;
				  minDist = dist;
			   }
			}
		 }
	  }
	  // don't need a null check because there has to be a junction
	  return best;
   }

   /*
	*
	*/
   public static Pose[] plotPathToJunction(Pose currentPosition, Junction destination) {
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
		 if (destination.position.getX() < currentPosition.x()) {
			x -= 0.5;
		 } else {
			x += 0.5;
		 }
	  }
	  if (y % 1 == 0) {
		 if (destination.position.getY() < currentPosition.y()) {
			y -= 0.5;
		 } else {
			y += 0.5;
		 }
	  }

	  points[0] = new Pose(
			  x * FieldNavConstants.GRID_SIZE,
			  y * FieldNavConstants.GRID_SIZE,
			  destination.position.getX() < x * FieldNavConstants.GRID_SIZE ? Math.PI : 0
	  );


	  // since the final destination is not in the middle of a grid square, we have to find the nearest one
	  double orientation;
	  Vector2D dest = destination.position;

	  // to left or to the right
	  if (destination.position.getX() < points[0].x()) {
		 dest = dest.add(new Vector2D(FieldNavConstants.GRID_SIZE / 2, 0));
		 orientation = Math.PI;
	  } else {
		 dest = dest.add(new Vector2D(-FieldNavConstants.GRID_SIZE / 2, 0));
		 orientation = 0;
	  }
	  if (destination.position.getY() < points[0].y()) {
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
		System.out.println("Point " + i + ": " + points[i]);
	  }
	  System.out.println("Points: " + points[0].toString() + ", " + points[1].toString() + ", " + points[2].toString());
	  return points;
   }
}
