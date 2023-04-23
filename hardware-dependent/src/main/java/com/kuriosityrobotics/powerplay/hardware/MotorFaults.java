package com.kuriosityrobotics.powerplay.hardware;

public class MotorFaults {
	public final boolean[] controlHub = new boolean[4];
	public final boolean[] expansionHub = new boolean[4];

	public MotorFaults() {
	}

	public boolean[] forHub(RobotConstants.LynxHub hub) {
		switch (hub) {
			case CONTROL_HUB:
				return controlHub;
			case EXPANSION_HUB:
				return expansionHub;
			default:
				throw new IllegalArgumentException("Unknown hub: " + hub);
		}
	}
}
