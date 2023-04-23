package com.kuriosityrobotics.powerplay.localisation.messages;

import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.math.TwistWithCovariance;
import com.kuriosityrobotics.powerplay.util.Instant;
import org.ojalgo.matrix.Primitive64Matrix;

import java.io.Serializable;
import java.util.Objects;

// TODO:  documentation for this class
public final class TimedTwistWithCovariance extends TwistWithCovariance implements Serializable {
	private final Instant time;

	public TimedTwistWithCovariance(double x, double y, double angular, Primitive64Matrix covariance, Instant time) {
	   super(x, y, angular, covariance);
	   this.time = time;
	}

	public static TimedTwistWithCovariance of (Twist velocity, Primitive64Matrix covariance, Instant time) {
		return new TimedTwistWithCovariance(velocity.x(), velocity.y(), velocity.angular(),covariance,time);
	}

	public static TimedTwistWithCovariance forCurrentTime(double x, double y, double angular, Primitive64Matrix covariance) {
	   return new TimedTwistWithCovariance(x, y, angular, covariance, Instant.now());
	}

	public Instant time() {
		return time;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (TimedTwistWithCovariance) obj;
		return Objects.equals(this.time, that.time) && super.equals(obj);
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, super.hashCode());
	}

	@Override
	public String toString() {
		return "[" + time + ", " + super.toString() + ']';
	}
}
