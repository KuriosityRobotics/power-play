package com.kuriosityrobotics.powerplay.bulkdata;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants;

import java.io.Serializable;

/**
 * The <code>RevHubBulkData</code> class is a container for bulk data from the Control Hub. Not to be
 * confused with the LynxModule.BulkData class, from which this class can be constructed.
 */
public class RevHubBulkData implements Serializable {

	public final RobotConstants.LynxHub origin;
	public int[] encoders = new int[4]; // 4 motors on CH/EH
	public final int[] velocities = new int[4]; // 4 motors on CH/EH
	public double[] analogInputs = new double[4]; // 4 analogue inputs on CH/EJ

	public boolean[] digitalInputs = new boolean[8];
	public boolean[] motorOverCurrentWarnings = new boolean[4];


	RevHubBulkData(RobotConstants.LynxHub origin) {
		this.origin = origin;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
	}
}
