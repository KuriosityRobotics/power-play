package com.kuriosityrobotics.powerplay.localisation.messages;

import static org.apache.commons.math3.util.FastMath.toDegrees;

import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.util.Instant;

import java.io.Serializable;
import java.util.Objects;

/** This represents an estimate of a position and velocity in free space. */
// TODO:  more documentation for this class
public final class LocalisationDatum implements Serializable {
	private final Instant stamp;

	private final Pose pose;
	private final Twist twist;

	public LocalisationDatum(Instant stamp, Pose pose, Twist twist) {
		this.stamp = stamp;
		this.pose = pose;
		this.twist = twist;
	}

	public static LocalisationDatum forCurrentTime(Pose pose, Twist twist) {
	   return new LocalisationDatum(Instant.now(), pose, twist);
	}

	public static LocalisationDatum zero() {
		return forCurrentTime(Pose.zero(), Twist.zero());
	}

	public Instant stamp() {
		return stamp;
	}

	public Pose pose() {
		return pose;
	}

	public Twist twist() {
		return twist;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (LocalisationDatum) obj;
		return Objects.equals(this.stamp, that.stamp)
				&& Objects.equals(this.pose, that.pose)
				&& Objects.equals(this.twist, that.twist);
	}

	@Override
	public int hashCode() {
		return Objects.hash(stamp, pose, twist);
	}

	@Override
	public String toString() {
		// Nicely format the pose and velocity, but omit the stamp
		return String.format(
				"%02.2f unit, %02.2f unit, %03.0f deg\n@ %02.2f unit/s, %02.2f unit/s, %03.1f deg/s",
				pose.x(),
				pose.y(),
				toDegrees(pose.orientation()),
				twist.x(),
				twist.y(),
				toDegrees(twist.angular()));
	}

	public LocalisationDatum metres() {
		return new LocalisationDatum(
			stamp,
			pose.metres(),
			twist.metres()
		);
	}
}
