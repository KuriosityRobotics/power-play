package com.kuriosityrobotics.powerplay.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@TeleOp
public class PortFinder extends LinearOpMode {
	@Override
	public void runOpMode() throws InterruptedException {
		var fLeft = (DcMotorEx) hardwareMap.dcMotor.get("fLeft"); //2 for
		var fRight = (DcMotorEx) hardwareMap.dcMotor.get("fRight"); //1 rev
		var bLeft = (DcMotorEx) hardwareMap.dcMotor.get("bLeft"); //3 for
		var bRight = (DcMotorEx) hardwareMap.dcMotor.get("bRight"); //0 rev

		fLeft.setDirection(DcMotorSimple.Direction.FORWARD);
		fRight.setDirection(DcMotorSimple.Direction.REVERSE);
		bLeft.setDirection(DcMotorSimple.Direction.FORWARD);
		bRight.setDirection(DcMotorSimple.Direction.REVERSE);

		waitForStart();
		while(opModeIsActive()){
			telemetry.addData("fLeft", fLeft.getCurrentPosition());
			telemetry.addData("fRight", fRight.getCurrentPosition());
			telemetry.addData("bLeft", bLeft.getCurrentPosition());
			telemetry.addData("bRight", bRight.getCurrentPosition());

			telemetry.update();
		}
	}
}
