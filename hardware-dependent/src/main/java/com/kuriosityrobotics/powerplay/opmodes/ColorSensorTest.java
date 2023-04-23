package com.kuriosityrobotics.powerplay.opmodes;

import android.graphics.Color;

import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.NormalizedColorSensor;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

@TeleOp
public class ColorSensorTest extends OrchestratedOpMode {

	NormalizedColorSensor colorSensor;
	NormalizedRGBA colors;
	final float[] hsvValues = new float[3];
	@Override
	public void runOpMode(HardwareOrchestrator orchestrator) {
		colorSensor = hardwareMap.get(NormalizedColorSensor.class, "sensor_color");

		waitForStart();
		while(opModeIsActive()){
			colors = colorSensor.getNormalizedColors();
			Color.colorToHSV(colors.toColor(), hsvValues);

			telemetry.addLine()
				.addData("Red", "%.3f", colors.red)
				.addData("Green", "%.3f", colors.green)
				.addData("Blue", "%.3f", colors.blue);
			telemetry.addLine()
				.addData("Hue", "%.3f", hsvValues[0])
				.addData("Saturation", "%.3f", hsvValues[1])
				.addData("Value", "%.3f", hsvValues[2]);
			telemetry.addData("Alpha", "%.3f", colors.alpha);
			if (colorSensor instanceof DistanceSensor) {
				telemetry.addData("Distance (cm)", "%.3f", ((DistanceSensor) colorSensor).getDistance(DistanceUnit.CM));
			}
			telemetry.update();
		}
	}
}
