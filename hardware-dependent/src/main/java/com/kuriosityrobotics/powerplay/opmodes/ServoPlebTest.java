package com.kuriosityrobotics.powerplay.opmodes;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants;
import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.HardwareProviderImpl;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp(name = "ServoTest", group = "Test")
@Disabled
public class ServoPlebTest extends OrchestratedOpMode {

	double position = 0.5;
	double position2 = 0.5;
	double position3 = 0.5;
	double position4 = 0.5;

	@Override
	public void runOpMode(HardwareOrchestrator orchestrator) {

		HardwareProviderImpl hardwareProvider = new HardwareProviderImpl(orchestrator, hardwareMap);

		var motor1 = hardwareProvider.motor(RobotConstants.LynxHub.CONTROL_HUB, 0, DcMotorSimple.Direction.REVERSE); // one of the intake motors (forward is in)
		var motor2 = hardwareProvider.motor(RobotConstants.LynxHub.CONTROL_HUB, 1, DcMotorSimple.Direction.FORWARD); // outtake motor (positive is up)
		var motor3 = hardwareProvider.motor(RobotConstants.LynxHub.CONTROL_HUB, 2, DcMotorSimple.Direction.REVERSE); // another one of the intake motors(forward is in)
		var motor4 = hardwareProvider.motor(RobotConstants.LynxHub.CONTROL_HUB, 3, DcMotorSimple.Direction.REVERSE); // outtake motor (negative is up)

		var servo1 = hardwareProvider.servo(RobotConstants.LynxHub.CONTROL_HUB, 0);
		var clawServo = hardwareProvider.servo(RobotConstants.LynxHub.CONTROL_HUB, 1);
		var armServo = hardwareProvider.servo(RobotConstants.LynxHub.CONTROL_HUB, 2);
		var ringServo = hardwareProvider.servo(RobotConstants.LynxHub.CONTROL_HUB, 3);

		double position = 0.5;
		double position2 = 0.5;
		double position3 = 0.5;
		double position4 = 0.5;

//		motor1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//		// motor1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//		motor2.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//		// motor2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//		motor3.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//		// motor3.setMode(DcMotor.RunMode.RUN_TO_POSITION);
//		motor4.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
//		// motor4.setMode(DcMotor.RunMode.RUN_TO_POSITION);

		while (opModeIsActive()) {
//			System.out.println("motor 1: " + motor1.getCurrentPosition());
//			System.out.println("motor 2: " + motor2.getCurrentPosition());
//			System.out.println("motor 3: " + motor3.getCurrentPosition());
//			System.out.println("motor 4: " + motor4.getCurrentPosition());
			position += gamepad1.left_stick_x * 0.0001;
			position2 += gamepad1.right_stick_x * 0.0001;
			position3 += gamepad2.left_stick_x * 0.0001;
			position4 += gamepad2.right_stick_x * 0.0001;
//
			servo1.setPosition(position);
			clawServo.setPosition(position2);
			armServo.setPosition(position3);
			ringServo.setPosition(position4);
			telemetry.addData("servo 1", position);
			telemetry.addData("servo 2", position2);
			telemetry.addData("servo 3", position3);
			telemetry.addData("servo 4", position4);

			System.out.println("ServoPlebtest/1 " + position);
			System.out.println("ServoPlebtest/2 " + position2);
			System.out.println("ServoPlebtest/3 " + position3);
			System.out.println("ServoPlebtest/4 " + position4);
//			if (gamepad1.a) {
//				clawServo.setPosition(1);
//				armServo.setPosition(0.9123);
//			}
//			if (gamepad1.b) {
//				clawServo.setPosition(0.29840645775395436);
//			}
//			if (gamepad1.y) {
//				ringServo.setPosition(1);
//				armServo.setPosition(0.0229);
//			}
//			if (gamepad1.x) {
//				clawServo.setPosition(1);
//			}
//			if (gamepad1.dpad_down) {
//				ringServo.setPosition(0.01);
			}

		}


	@RunPeriodically()
	public void update(){
	}
}