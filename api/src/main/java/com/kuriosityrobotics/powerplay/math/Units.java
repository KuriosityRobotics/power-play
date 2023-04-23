package com.kuriosityrobotics.powerplay.math;

public class Units {
	public static final double INCHES_TO_METRES = 0.0254;
	public static final double METRES_TO_INCHES = 1 / INCHES_TO_METRES;
	public static final double RADIANS_TO_DEGREES = 180 / Math.PI;
	public static final double DEGREES_TO_RADIANS = 1 / RADIANS_TO_DEGREES;

	public static final double NEWTONS_TO_OUNCE_FORCE = 3.596943;
	public static final double OUNCE_FORCE_TO_NEWTONS = 1 / NEWTONS_TO_OUNCE_FORCE;
	public static final double POUNDS_TO_OUNCE = 16;
	public static final double OUNCE_TO_POUNDS = 1 / POUNDS_TO_OUNCE;
	public static final double NEWTON_METRES_TO_OZ_INCHES =
			NEWTONS_TO_OUNCE_FORCE * METRES_TO_INCHES;
}
