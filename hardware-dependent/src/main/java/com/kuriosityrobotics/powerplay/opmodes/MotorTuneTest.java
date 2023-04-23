package com.kuriosityrobotics.powerplay.opmodes;

import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.EXPANSION_HUB;
import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD;
import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE;

import com.kuriosityrobotics.powerplay.pubsub.HardwareOrchestrator;
import com.kuriosityrobotics.powerplay.pubsub.MotorOrEncoder;
import com.kuriosityrobotics.powerplay.pubsub.OrchestratedOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.concurrent.ExecutionException;

@TeleOp
@Disabled
public class MotorTuneTest extends OrchestratedOpMode {
	@Override
	protected void initOpMode(HardwareOrchestrator orchestrator) {
		super.initOpMode(orchestrator);
		orchestrator.stopAllNodes();
	}
	public static final double clawOpen = 0.216;
	public static final double clawClosed = 0.588;
	public static final double armDown = 0.124;
	public static final double armUp = 1;

	public static final double ringInside = 0;
	public static final double ringDepo = 0.714;
	@Override
	protected void runOpMode(HardwareOrchestrator orchestrator) throws InterruptedException, ExecutionException {
		var left = hardwareMap.get(DcMotor.class, "leftOuttake");
		var right = hardwareMap.get(DcMotor.class, "rightOuttake");
		var leftIn = hardwareMap.get(DcMotor.class, "3");
		var rightIn = hardwareMap.get(DcMotor.class, "4");
		var servo = hardwareMap.get(Servo.class, "1");
		var servo1 = hardwareMap.get(Servo.class, "2");
		var servo2 = hardwareMap.get(Servo.class, "3");
		var mystery = hardwareMap.get(Servo.class, "4");//c, su, 4
			// change between one and 2 then left trigger to move servo

		super.runOpMode(orchestrator);
		leftIn.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		rightIn.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		leftIn.setTargetPosition(0);
		rightIn.setTargetPosition(0);
		leftIn.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
		rightIn.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
//		leftIn.setPower(1);
//		rightIn.setPower(1);
		rightIn.setDirection(REVERSE);
		leftIn.setDirection(FORWARD);

		leftIn.setDirection(FORWARD);
		rightIn.setDirection(REVERSE);
		double position = servo.getPosition();
		double position1 = servo1.getPosition();
		double position2 = servo2.getPosition();


		waitForStart();
		while (opModeIsActive()) {
			mystery.setPosition(0.5);
//			if (gamepad1.a){
//				leftIn.setPower(1);
//				System.out.println("left: " + leftIn.getCurrentPosition() + ", " + leftIn.getTargetPosition());
//			}else{
//				leftIn.setPower(0);
//			}
//			if (gamepad1.b){
//				rightIn.setPower(1);
//				System.out.println("right: " + rightIn.getCurrentPosition() + ", " + rightIn.getTargetPosition());
//			}else{
//				rightIn.setPower(0);
//			}
//			if (gamepad1.x){
//				leftIn.setPower(1);
//			}else{
//				leftIn.setPower(0);
//			}
			if (gamepad1.a){
				servo.setPosition(ringDepo);
			}if (gamepad1.b){
				servo.setPosition(ringInside);
			} if (gamepad1.x){
				servo1.setPosition(armUp);
			} if (gamepad1.y){
				servo1.setPosition(armDown);
			} if (gamepad1.dpad_up){
				servo2.setPosition(clawOpen);
			} if (gamepad1.dpad_down){
				servo2.setPosition(clawClosed);
			}

			if (gamepad2.a){
				left.setPower(1);
			}else{
				left.setPower(0);
			}
			if (gamepad2.b){
				right.setPower(1);
			}else{
				right.setPower(0);
			}
			if (gamepad2.x){
				rightIn.setPower(1);
			}else{
				rightIn.setPower(0);
			}
			if (gamepad2.y){
				leftIn.setPower(1);
			}else{
				leftIn.setPower(0);
			}

//			telemetry.addData("left trigger", gamepad1.left_stick_x);
//			telemetry.addData("servo port", servo.getPortNumber());
			telemetry.addData("servo position", servo.getPosition());
			telemetry.addData("position", position);
			telemetry.addData("servo1 position", servo1.getPosition());
			telemetry.addData("position1", position1);
			telemetry.addData("servo2 position", servo2.getPosition());
			telemetry.addData("position2", position2);
			telemetry.addData("left outtake", left.getCurrentPosition());
			telemetry.addData("right outtake", right.getCurrentPosition());
			telemetry.addData("left intake", leftIn.getCurrentPosition());
			telemetry.addData("right intake", rightIn.getCurrentPosition());
			telemetry.update();
		}
	}
}
