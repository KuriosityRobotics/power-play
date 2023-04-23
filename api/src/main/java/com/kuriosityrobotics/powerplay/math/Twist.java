package com.kuriosityrobotics.powerplay.math;

import com.kuriosityrobotics.powerplay.util.Duration;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.ojalgo.matrix.Primitive64Matrix;

import java.io.Serializable;
import java.util.Objects;

import static com.kuriosityrobotics.powerplay.math.Matrices.diag;
import static com.kuriosityrobotics.powerplay.util.StringUtils.toDisplayString;

/**
 * This expresses velocity in free space broken into its linear and angular parts
 */
public class Twist extends Vector2D implements Serializable {
	private static final Twist ZERO = new Twist(0, 0, 0);
	private final double angular;

	/**
	 * @param angular theta radians per second
	 */
	public Twist(double x, double y, double angular) {
		super(x, y);
		this.angular = angular;
	}

	public static Twist zero() {
		return ZERO;
	}

    public static Twist of(double xVel, double yVel, double thetaVel) {
		return new Twist(xVel, yVel, thetaVel);
    }

    public Pose apply(Pose pose, Duration duration) {
		var newPosition =
			pose.add(scalarMultiply(duration.toMillis() / 1000.));
		var newAngle = pose.orientation() + this.angular * (duration.toMillis() / 1000.);

		return new Pose(newPosition.getX(), newPosition.getY(), newAngle);
	}

	@Override
	public Twist scalarMultiply(double a) {
		return new Twist(x() * a, y() * a, angular * a);
	}

	public double angular() {
		return angular;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Twist) obj;
		return this.getX() == that.getX() &&
			this.getY() == that.getY() &&
			this.angular == that.angular;
	}

	public double x() {
		return getX();
	}

	public double y() {
		return getY();
	}

	public double getAngular() {
		return this.angular;
	}

	public Twist subtract(Twist other) {
		return new Twist(x() - other.x(), y() - other.y(), angular() - other.angular());
	}

	@Override
	public int hashCode() {
		return Objects.hash(x(), y(), angular);
	}

	@Override
	public String toString() {
		return "[" + toDisplayString(x()) + ", " + toDisplayString(y()) + ", " + toDisplayString(angular) + "]";
	}

	public double[] getData() {
		return new double[]{x(), y(), angular};
	}

	public Primitive64Matrix toColumnVector() {
		return Primitive64Matrix.FACTORY.column(getData());
	}

	public Primitive64Matrix toColumnVectorMetres() {
		return diag(1, 1, 1.0).multiply(toColumnVector());
	}

	@Override
	public double[] toArray() {
		return getData();
	}

	public Twist metres() {
		return new Twist(x(), y(), angular);
	}

	public Twist rotate(double angle) {
		return new Twist(
			x() * Math.cos(angle) - y() * Math.sin(angle),
			x() * Math.sin(angle) + y() * Math.cos(angle),
			angular
		);
	}
}
