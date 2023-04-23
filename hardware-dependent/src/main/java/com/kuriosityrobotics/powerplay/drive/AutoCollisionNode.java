package com.kuriosityrobotics.powerplay.drive;


import static java.lang.Math.*;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.math.Twist;
import com.kuriosityrobotics.powerplay.pubsub.AnalogInput;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.util.Instant;

public class AutoCollisionNode extends Node {
	private static final double SENSOR_MIN_DISTANCE = 3;
	private static final double RAM_VECTOR_SPEED = 10;
	private static final long RAM_WAIT_LENGTH = 1000;

	private boolean isAdjusting = false;
	private final double startAngle;

	@LastMessagePublished(topic = "localisation")
	private LocalisationDatum localisation = new LocalisationDatum(Instant.now(), Pose.zero(), new Twist(0, 0, 0));

	@AnalogInput(hub = RobotConstants.LynxHub.EXPANSION_HUB, channel = 3)
	private com.qualcomm.robotcore.hardware.AnalogInput frontLeft;
	@AnalogInput(hub = RobotConstants.LynxHub.CONTROL_HUB, channel = 1)
	private com.qualcomm.robotcore.hardware.AnalogInput frontRight;
	@AnalogInput(hub = RobotConstants.LynxHub.EXPANSION_HUB, channel = 2)
	private com.qualcomm.robotcore.hardware.AnalogInput backLeft;
	@AnalogInput(hub = RobotConstants.LynxHub.CONTROL_HUB, channel = 3)
	private com.qualcomm.robotcore.hardware.AnalogInput backRight;

	private final int accuracy = 10;
	private int currentIndex = -1;
	private double[] flDistances = new double[accuracy];
	private double flAverage = 0.0;
	private double[] frDistances = new double[accuracy];
	private double frAverage = 0.0;
	private double[] blDistances = new double[accuracy];
	private double blAverage = 0.0;
	private double[] brDistances = new double[accuracy];
	private double brAverage = 0.0;

	private final boolean isBlue;

	public AutoCollisionNode(Orchestrator orchestrator, boolean isBlue) {
		super(orchestrator);
		this.isBlue = isBlue;
		this.startAngle = localisation.pose().orientation();
	}

	@RunPeriodically(maxFrequency = 40)
	public void update() throws InterruptedException {
		if(isAdjusting) return;

		currentIndex++;
		int i = (currentIndex % accuracy);
		try{
			flDistances[i] = cmFromVoltage(frontLeft.getVoltage());
			frDistances[i] = cmFromVoltage(frontRight.getVoltage());
			blDistances[i] = cmFromVoltage(backLeft.getVoltage());
			brDistances[i] = cmFromVoltage(backRight.getVoltage());
			flAverage = (flAverage * accuracy + flDistances[i] - flDistances[(i + 1) % accuracy]) / accuracy;
			frAverage = (frAverage * accuracy + frDistances[i] - frDistances[(i + 1) % accuracy]) / accuracy;
			blAverage = (blAverage * accuracy + blDistances[i] - blDistances[(i + 1) % accuracy]) / accuracy;
			brAverage = (brAverage * accuracy + brDistances[i] - brDistances[(i + 1) % accuracy]) / accuracy;
		}catch (Exception e){
			System.out.println("Error: " + e.getMessage() + ". " + i);
			e.printStackTrace();
		}

		if(
			currentIndex > accuracy &&
				isBlue &&
				(flAverage < SENSOR_MIN_DISTANCE ||
					blAverage < SENSOR_MIN_DISTANCE)
		){
			calculateNewTrajectory(PI / 2);
		}else if(
			currentIndex > accuracy &&
				!isBlue &&
				(frAverage < SENSOR_MIN_DISTANCE ||
					brAverage < SENSOR_MIN_DISTANCE)
		){
			calculateNewTrajectory(-PI / 2);
		}
	}

	private Twist getRamPoint(double rotationAngle){
		var angle = localisation.pose().orientation() + rotationAngle - startAngle;
		System.out.println("robot is at " + toDegrees(angle) + " degrees");
		Twist ramVec = new Twist(sin(angle), cos(angle), 0).scalarMultiply(RAM_VECTOR_SPEED);
		System.out.println("ram vector is " + ramVec + ". rotating vector by " + rotationAngle);
		return ramVec;
	}

	private void calculateNewTrajectory(double angle) throws InterruptedException {
		isAdjusting = true;
		System.out.println("Collision detected, calculating new trajectory. current position is " + localisation.pose());
		Twist ram = getRamPoint(angle);
		orchestrator.dispatch("mpc/setVelocity", ram);
		orchestrator.dispatch("mpc/setLinearVelWeight", 10000);
		orchestrator.dispatch("mpc/setAngularVelWeight", 10000);
		Thread.sleep(RAM_WAIT_LENGTH);
		orchestrator.dispatch("mpc/setLinearVelWeight", 0);
		orchestrator.dispatch("mpc/setAngularVelWeight", 0);
		orchestrator.dispatch("mpc/setVelocity", Twist.zero());
		Thread.sleep(RAM_WAIT_LENGTH);
		isAdjusting = false;
	}

	public static double cmFromVoltage(double voltage) {
		return inchesFromVoltage(voltage) * 2.54;
	}

	public static double inchesFromVoltage(double voltage) {
		return 12.3615 * exp(-2.59267 * voltage) + 0.817458;
	}
}

// Collision detected, calculating new trajectory. current position is Pose[1.68255, 1.18539, -1.07478]
// robot is at 28.41980351124658 degrees
// ram vector is [1.43, 2.64, 0]. rotating vector by 1.5707963267948966
