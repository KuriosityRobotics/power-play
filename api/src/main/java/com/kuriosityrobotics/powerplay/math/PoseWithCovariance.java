package com.kuriosityrobotics.powerplay.math;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.ojalgo.matrix.Primitive64Matrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PoseWithCovariance extends Pose implements Serializable {
	private transient Primitive64Matrix covariance;

	/**
	 * A representation of pose in free space, composed of position, orientation and a covariance.
	 *
	 * @param orientation
	 * @param covariance
	 */
	protected PoseWithCovariance(double x, double y, double orientation, Primitive64Matrix covariance) {
		super(x, y, orientation);
		this.covariance = covariance;
	}

	public static PoseWithCovariance of(Pose pose, Primitive64Matrix covariance) {
		if (covariance.getColDim() != 3 || covariance.getRowDim() != 3)
			throw new IllegalArgumentException(
					"Pose covariance must be 6x6, but was "
							+ covariance.getRowDim()
							+ "x"
							+ covariance.getColDim());

		return new PoseWithCovariance(pose.x(), pose.y(), pose.orientation(), covariance);
	}

	public final Primitive64Matrix covariance() {
		return covariance;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeObject(covariance.toRawCopy2D());
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		this.covariance = Primitive64Matrix.FACTORY.rows((double[][]) stream.readObject());
	}
}
