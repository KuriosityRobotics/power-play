package com.kuriosityrobotics.powerplay.hardware;

public class OverCurrentFault extends HardwareException {
	public OverCurrentFault(String diagnosticMessage) {
		super(diagnosticMessage);
	}
}
