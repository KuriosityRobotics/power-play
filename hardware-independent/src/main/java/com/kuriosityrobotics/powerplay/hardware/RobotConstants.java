package com.kuriosityrobotics.powerplay.hardware;

import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.*;
import static com.kuriosityrobotics.powerplay.util.Units.CM;

/**
 * A simple class storing constants relating to the name and ports of all Robot-related
 * electronics(control hub, expansion hub, motors, etc.)
 */
public class RobotConstants {
   // hubs
   public static final String CONTROL_HUB_NAME = "Control Hub";
   public static final String EXPANSION_HUB_NAME = "Expansion Hub";

   // deadwheels

//	this year's robot config (R1)
	public static final int LEFT_ODO_PORT = 3;
	public static final int MECANUM_ODO_PORT = 2;

	public static final int LEFT_ODO_DIR = 1;
	public static final int MECANUM_ODO_DIR = 1;

	public static final double WHEEL_RADIUS = 2.412492 * CM;
	public static final double DISTANCE_TO_FORWARDS_ENCODER =  -8.07 * CM; // distance leftwards from center of robot to forwards encoder
	public static final double DISTANCE_TO_SDEWAYS_ENCODER = 7.97 * CM; // distance forwards from center of robot to sideways encoder

   // servos (port number)

   // other sensors + devices in i2c ports

   public enum LynxHub {
	  CONTROL_HUB(CONTROL_HUB_NAME),
	  EXPANSION_HUB(EXPANSION_HUB_NAME);

	  private final String hardwareName;

	  LynxHub(String hardwareName) {
		 this.hardwareName = hardwareName;
	  }

	  public String hardwareName() {
		 return hardwareName;
	  }
   }
}
