package com.kuriosityrobotics.powerplay.pubsub.bridge.message;

import com.kuriosityrobotics.powerplay.util.Instant;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/** A message containing a robot's {@link RobotDetails} and, optionally, a caption and a datum. */
public class NetworkMessage implements Serializable {
	private final RobotDetails robotDetails;

	private final String caption;
	private final Object datum;
	
	private final Instant constructionTime;

	private NetworkMessage(
			RobotDetails robotDetails, String caption, Object datum) {
		this.robotDetails = robotDetails;
		this.caption = caption;
		this.datum = datum;
		this.constructionTime = Instant.now();
	}

	/**
	 * Creates a NetworkMessage with the specified {@link RobotDetails}, caption and datum
	 *
	 * @param robotDetails details of the robot from whom the message is to be broadcast
	 * @param caption a caption giving context to the attached datum
	 * @param datum included datum
	 * @return a newly created {@link NetworkMessage}
	 */
	public static NetworkMessage datum(
			RobotDetails robotDetails,
			String caption,
			Object datum) {
		return new NetworkMessage(robotDetails, caption, datum);
	}

	/**
	 * Creates a NetworkMessage with the specified {@link RobotDetails} and text caption
	 *
	 * @param robotDetails details of the robot from whom the message is to be broadcast
	 * @param caption a text caption
	 * @return
	 */
	public static NetworkMessage text(RobotDetails robotDetails, String caption) {
		return new NetworkMessage(robotDetails, caption, null);
	}

	public String caption() {
		return caption;
	}

	public Object datum() {
		return datum;
	}

	public RobotDetails robotDetails() {
		return robotDetails;
	}

   public Instant constructionTime() {
	  return constructionTime;
   }

   @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof NetworkMessage)) return false;
		NetworkMessage that = (NetworkMessage) o;
		return Objects.equals(caption, that.caption) && Objects.equals(datum, that.datum);
	}

	@Override
	public int hashCode() {
		return Objects.hash(caption, datum);
	}

	@Override
	public String toString() {
		if (datum != null) return String.format("[%10s] %10s:  %s%n", robotDetails, caption, datum);
		else return String.format("[%10s] %s", robotDetails, caption);
	}
}
