package com.kuriosityrobotics.powerplay.drive;

import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.util.Instant;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class DriveLogger extends Node {
	private PrintWriter out;

	@LastMessagePublished(topic = "localisation")
	private LocalisationDatum d = LocalisationDatum.zero();

	@LastMessagePublished(topic = "batteryVoltage")
	private double batteryVoltage = 0;

	@LastMessagePublished(topic = "motorPowers")
	private MotorPowers motorPowers = MotorPowers.zero();

	private final Instant startTime = Instant.now();


	/**
	 * Registers the node with the orchestrator using {@link
	 * Orchestrator#markNodeBeingConstructed(Node)}. This will NOT start calling the update
	 * function, and will give this node instance a placeholder name
	 *
	 * <p>This exists so telemetry works inside the constructor
	 *
	 * @param orchestrator the orchestrator on which to start the node
	 */
	public DriveLogger(Orchestrator orchestrator) {
		super(orchestrator);
		try {
			out = new PrintWriter("/sdcard/FIRST/output.csv");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		out.println("time,battery_voltage,fl,fr,bl,br,x_position,y_position,angle,x_velocity,y_velocity,angular_velocity");
	}

	private final Object csvLock_ = new Object();
	@RunPeriodically(maxFrequency = 20)
	public void updateCSV() {
		synchronized (csvLock_) {
			out.printf(
				"%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f%n",
				Instant.now().since(startTime).toSeconds(),
				batteryVoltage,
				motorPowers.powerFrontLeft(),
				motorPowers.powerFrontRight(),
				motorPowers.powerBackLeft(),
				motorPowers.powerBackRight(),
				d.pose().x(),
				d.pose().y(),
				d.pose().orientation(),
				d.twist().x(),
				d.twist().y(),
				d.twist().angular()
			);
		}
	}

	@Override
	public void close() {
		super.close();
		out.flush();
	}
}
