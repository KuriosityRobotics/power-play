package com.kuriosityrobotics.powerplay.opmodes;

import com.kuriosityrobotics.powerplay.math.Line;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxServoController;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;

@TeleOp
public class ClawTest extends LinearOpMode {
	@Override
	public void runOpMode() throws InterruptedException {
		var servo = hardwareMap.get(Servo.class, "1");

		double position = 0.5;
		waitForStart();
		while (opModeIsActive()) {
			position += gamepad1.left_stick_x * 0.0001;
			servo.setPosition(position);
			telemetry.addData("pos", position);
			telemetry.update();
		}

	}
}
