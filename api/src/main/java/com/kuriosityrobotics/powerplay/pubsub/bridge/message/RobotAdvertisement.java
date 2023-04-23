package com.kuriosityrobotics.powerplay.pubsub.bridge.message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class RobotAdvertisement implements Serializable {
	private final RobotDetails robotDetails;
	private final InetSocketAddress address;

	public RobotAdvertisement(RobotDetails robotDetails, InetSocketAddress address) {
		this.robotDetails = robotDetails;
		this.address = address;
	}

	public RobotDetails robotDetails() {
		return robotDetails;
	}

	public InetSocketAddress address() {
		return address;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RobotAdvertisement) {
			return robotDetails.equals(((RobotAdvertisement) obj).robotDetails);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return robotDetails.hashCode();
	}

	@Override
	public String toString() {
		return "RobotAdvertisement{" + "robotDetails=" + robotDetails + '}';
	}
}
