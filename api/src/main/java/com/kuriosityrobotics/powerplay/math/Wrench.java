package com.kuriosityrobotics.powerplay.math;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.ojalgo.matrix.Primitive64Matrix;

/**
 * This represents force in free space, separated into its linear and angular parts
 */
public class Wrench extends Vector2D {

   /**
	* The angular part of the force (newtons-metres)
	*
	* @see <a href="https://en.wikipedia.org/wiki/Torque">Torque</a>
	*/
   private final double torque;

   public Wrench(double x, double y, double torque) {
	  super(x, y);
	  this.torque = torque;
   }

   public static Wrench of(double forceX, double forceY, double torque) {
	  return new Wrench(forceX, forceY, torque);
   }

   /**
	* @return the angular component of the wrench (newtons-metres)
	*/
   public double torque() {
	  return torque;
   }

   public double x() {
	  return getX();
   }

   public double y() {
	  return getY();
   }

   public Primitive64Matrix toColumnVector() {
	  return Primitive64Matrix.FACTORY.column(x(), y(), torque);
   }
}
