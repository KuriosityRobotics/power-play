package com.kuriosityrobotics.powerplay.io;

import com.kuriosityrobotics.powerplay.hardware.HardwareException;
import com.kuriosityrobotics.powerplay.pubsub.*;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunnableAction;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.util.Instant;
import com.qualcomm.robotcore.hardware.ColorRangeSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.Servo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.CONTROL_HUB;
import static com.kuriosityrobotics.powerplay.io.Claw.ClawPosition.OPEN;
import static com.kuriosityrobotics.powerplay.io.Claw.ClawPosition.SHUT;
import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD;
import static com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE;

import static com.kuriosityrobotics.powerplay.io.IntakeExtensionSlides.IntakeSlidePosition;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class IntakeOuttakeNode extends Node {
	@Servomotor(hub = CONTROL_HUB, port = 4)
	private Servo clawServo_;

	@Servomotor(hub = CONTROL_HUB, port = 1)
	private Servo leftArmServo_;
	@Servomotor(hub = CONTROL_HUB, port = 2)
	private Servo rightArmServo_;

	@MotorOrEncoder(hub = CONTROL_HUB, port = 2, direction = REVERSE)
	private DcMotorEx rightIntakeExtension;

	@MotorOrEncoder(hub = CONTROL_HUB, port = 0, direction = REVERSE)
	private DcMotorEx leftIntakeExtension;

	private final IntakeExtensionSlides intakeExtensionSlides;
	private final Claw intakeClaw;
	private final IntakeArm intakeArm;

	@MotorOrEncoder(hub = CONTROL_HUB, port = 3, direction = REVERSE)
	private DcMotorEx leftOuttakeExtension;
	@MotorOrEncoder(hub = CONTROL_HUB, port = 1, direction = FORWARD)
	private DcMotorEx rightOuttakeExtension;
	@Servomotor(hub = CONTROL_HUB, port = 3)
	private Servo ringServo;

	@NamedHardware("sensor_color")
	private ColorRangeSensor slidesDistanceSensor;


	private final OuttakeExtensionSlides outtakeExtensionSlides;
	private final Ring transferRing;


	private volatile PickupTarget pickupTarget = PickupTarget.ONE_STACK;
	private DepositTarget depositTarget = DepositTarget.HIGH_POLE;


	public IntakeOuttakeNode(Orchestrator orchestrator) {
		super(orchestrator);
		this.intakeExtensionSlides = new IntakeExtensionSlides(orchestrator, rightIntakeExtension, leftIntakeExtension, slidesDistanceSensor);
		this.intakeClaw = new Claw(clawServo_);
		this.intakeArm = new IntakeArm(orchestrator, leftArmServo_, rightArmServo_);

		this.outtakeExtensionSlides = new OuttakeExtensionSlides(leftOuttakeExtension, rightOuttakeExtension);
		this.transferRing = new Ring(ringServo);

		rightIntakeExtension.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		leftOuttakeExtension.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
		rightOuttakeExtension.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
	}

	@SubscribedTo(topic = "hardware/arm")
	void arm() throws ExecutionException, InterruptedException {
		rightIntakeExtension.setTargetPosition(rightIntakeExtension.getCurrentPosition());
		leftOuttakeExtension.setTargetPosition(leftOuttakeExtension.getCurrentPosition());
		rightOuttakeExtension.setTargetPosition(rightOuttakeExtension.getCurrentPosition());

		rightIntakeExtension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
		leftOuttakeExtension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
		rightOuttakeExtension.setMode(DcMotor.RunMode.RUN_TO_POSITION);

		leftOuttakeExtension.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, new PIDFCoefficients(0, 0, 0, 40));
		rightOuttakeExtension.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, new PIDFCoefficients(0, 0, 0, 40));

		rightIntakeExtension.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, new PIDFCoefficients(30, 0, 0, 70));
		rightIntakeExtension.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

		rightIntakeExtension.setPower(1);
		leftOuttakeExtension.setPower(1);
		rightOuttakeExtension.setPower(1);

		rightIntakeExtension.setCurrentAlert(4, CurrentUnit.AMPS); // TODO:  make sure this is the right value
		leftOuttakeExtension.setCurrentAlert(5, CurrentUnit.AMPS);
		rightOuttakeExtension.setCurrentAlert(5, CurrentUnit.AMPS);

		intakeArm.toNeutral();
		intakeClaw.goToClawPosition(SHUT);
	}

	@RunPeriodically(maxFrequency = 10)
	void breakpointfunny() {
		var a = 1;
	}

	@SubscribedTo(topic = "io/intake/target")
	void changeConeStackHeight(PickupTarget pickupTarget) throws ExecutionException, InterruptedException {
		this.pickupTarget = pickupTarget;
		intakeArm.toPickup(pickupTarget);
	}

	@SubscribedTo(topic = "io/outtake/target")
	void changeDepositTarget(DepositTarget depositTarget) {
		this.depositTarget = depositTarget;
	}

	public void setExtension(int extension) {
		rightIntakeExtension.setTargetPosition(extension);
	}

	@RunnableAction(actionName = "io/intake/nearPickup")
	public void nearPickup() throws InterruptedException, ExecutionException {
		var extensionMovement = orchestrator.startActionAsync(() -> intakeExtensionSlides.goToPosition(IntakeSlidePosition.HARDSTOP));
		prepareArmAndClawForIntaking();
		extensionMovement.get();
	}

	@RunnableAction(actionName = "io/intake/farPickup")
	public void farPickup() throws InterruptedException, ExecutionException {
		var extensionMovement = orchestrator.startActionAsync(() -> intakeExtensionSlides.goToPosition(IntakeSlidePosition.EXTENDED_TELEOP));
		prepareArmAndClawForIntaking();
		extensionMovement.get();
	}

	@RunnableAction(actionName = "io/intake/farExtend")
	public void farExtend() throws InterruptedException, ExecutionException {
		var armMovement = orchestrator.startActionAsync(() -> intakeArm.toNeutral());
		var extensionMovement = orchestrator.startActionAsync(() -> intakeExtensionSlides.goToPosition(IntakeSlidePosition.EXTENDED_TELEOP));
		prepareArmAndClawForIntaking();
		extensionMovement.get();
		armMovement.get();
	}

	@RunnableAction(actionName = "io/intake/beaconHalfTransfer")
	public void beaconHalfTransfer() throws InterruptedException, ExecutionException {
		intakeClaw.goToClawPosition(SHUT);

		var extensionMovement = orchestrator.startActionAsync(() -> intakeExtensionSlides.goToPosition(IntakeSlidePosition.HARDSTOP));
		var armMovement = orchestrator.startActionAsync(intakeArm::toNeutral);

		armMovement.get();
		extensionMovement.get();
	}

	private void prepareArmAndClawForIntaking() throws InterruptedException, ExecutionException {
		var armMovement = orchestrator.startActionAsync(() -> intakeArm.toPickup(pickupTarget));

//		while (!intakeExtensionSlides.isHardstopEngaged()) { // wait till it's safe to open the claw
//			orchestrator.waitForMessage("bulkData");
//			if (!intakeExtensionSlides.isBusy() && !intakeExtensionSlides.isHardstopEngaged())
//				throw new RuntimeException("Intaking requested, but intake is neither past the hardstop nor in the process of moving past it");
//		}

		intakeClaw.goToClawPosition(OPEN);
		armMovement.get();
	}


	@RunnableAction(actionName = "io/arm/neutral")
	public void neutral() throws InterruptedException, ExecutionException {
		intakeArm.toNeutral();
	}

	@RunnableAction(actionName = "io/transfer")
	public void transfer() throws InterruptedException, ExecutionException {
		if (depositTarget == DepositTarget.GROUND_JUNCTION){
			intakeClaw.goToClawPosition(SHUT);

			var armMovement = orchestrator.startActionAsync(intakeArm::toGroundJunctionDeposit);
			var slideMovement = orchestrator.startActionAsync(() -> intakeExtensionSlides.goToPosition(IntakeSlidePosition.GROUND_JUNCTION_DEPOSIT));

			armMovement.get();
			slideMovement.get();
		}else{
			try {
				var ringTransferMovement = orchestrator.startActionAsync(() -> transferRing.goToRingPosition(Ring.RingPosition.TRANSFER));
//				var outtake = orchestrator.startActionAsync(() -> outtakeExtensionSlides.goToPosition(OuttakeExtensionSlides.OuttakeSlidePosition.RETRACTED));

				intakeClaw.goToClawPosition(SHUT);
				intakeArm.toHover(pickupTarget); // move the arm up a bit

				// finish the rest of the arm movement and retract the intake slides in parallel

				var armMovement = orchestrator.startActionAsync(intakeArm::toTransfer);
				var retractMovement = orchestrator.startActionAsync(() -> intakeExtensionSlides.goToPosition(IntakeSlidePosition.HARDSTOP));

//				outtake.get();
				armMovement.get();
				retractMovement.get();
				ringTransferMovement.get();

				// wait for cone to finish boucning around
				Thread.sleep(400);

				intakeClaw.goToClawPosition(OPEN);
				orchestrator.dispatch("intake/transferComplete", "");
			} finally {
				orchestrator.startActionAsync(() -> {
					intakeArm.toNeutral();
					markConeTransferred();
				});
			}
		}
	}

	@RunnableAction(actionName = "io/cycle")
	public void cycle() throws InterruptedException, ExecutionException, HardwareException {
		transfer();
		deposit();
	}

	@RunnableAction(actionName = "io/deposit")
	public void deposit() throws ExecutionException, InterruptedException, HardwareException {
		switch (depositTarget) {
			case GROUND_JUNCTION:
				depositGroundJunction();
				break;
			case LOW_POLE:
				depositLowPole();
				break;
			case MEDIUM_POLE:
				depositMiddlePole();
				break;
			case HIGH_POLE:
				depositHighPole();
				break;
		}
	}

	private void depositGroundJunction() throws InterruptedException, ExecutionException {
		intakeArm.toFlat();
		intakeClaw.goToClawPosition(OPEN);
		intakeArm.toNeutral();
	}

	private void depositLowPole() throws ExecutionException, InterruptedException, HardwareException {
		depositPole(OuttakeExtensionSlides.OuttakeSlidePosition.LOW_POLE);
	}

	private void depositMiddlePole() throws ExecutionException, InterruptedException, HardwareException {
		depositPole(OuttakeExtensionSlides.OuttakeSlidePosition.MEDIUM_POLE);
	}

	private void depositHighPole() throws ExecutionException, InterruptedException, HardwareException {
		depositPole(OuttakeExtensionSlides.OuttakeSlidePosition.HIGH_POLE);
	}

	private void depositPole(OuttakeExtensionSlides.OuttakeSlidePosition position) throws ExecutionException, InterruptedException, HardwareException {
		outtakeExtensionSlides.goToPosition(position);
		transferRing.goToRingPosition(Ring.RingPosition.DEPOSIT);

		// wait for pole to finish wobbling
		Thread.sleep(200);

		// retract
		var ringMovement = orchestrator.startActionAsync(() -> transferRing.goToRingPosition(Ring.RingPosition.TRANSFER));
		var slideMovement = orchestrator.startActionAsync(() -> outtakeExtensionSlides.goToPosition(OuttakeExtensionSlides.OuttakeSlidePosition.RETRACTED));

		ringMovement.get();
		slideMovement.get();
	}

	private volatile Instant coneHandleTime;

	private final Lock coneCheckLock = new ReentrantLock();

	private void markConeTransferred() throws InterruptedException {
		coneCheckLock.lockInterruptibly();
		try {
			coneHandleTime = null;
		} finally {
			coneCheckLock.unlock();
		}
	}

	@RunnableAction(actionName = "io/intake/alignForUntip")
	public void alignForUntip() throws InterruptedException, ExecutionException {
		intakeArm.alignForUntip();
	}

	@RunnableAction(actionName = "io/intake/untip")
	public void untip() throws InterruptedException, ExecutionException {
		intakeArm.attemptUntip();
	}
}
