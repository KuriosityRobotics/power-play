package com.kuriosityrobotics.powerplay.pubsub;

import static com.kuriosityrobotics.powerplay.pubsub.OrchestratorImpl.sneakyThrow;

import android.app.Activity;

import com.kuriosityrobotics.powerplay.debug.DriverHubTelemetry;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.qualcomm.ftccommon.FtcEventLoop;
import com.qualcomm.ftccommon.UpdateUI;
import com.qualcomm.hardware.HardwareFactory;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegister;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar;
import com.qualcomm.robotcore.exception.RobotCoreException;

import java.util.Random;

public class OrchestratorBoundEventLoop extends FtcEventLoop {
	private static OrchestratorBoundEventLoop INSTANCE;
	private static final Random random = new Random();

	private volatile HardwareOrchestrator hardwareOrchestrator;
	private DriverHubTelemetry driverHubTelemetry;

	private volatile boolean hardwareInitialised = false;

	public OrchestratorBoundEventLoop(HardwareFactory hardwareFactory, OpModeRegister userOpmodeRegister, UpdateUI.Callback callback, Activity activityContext) {
		super(hardwareFactory, userOpmodeRegister, callback, activityContext);
		try {
			this.hardwareOrchestrator = HardwareOrchestrator.create(ftcEventLoopHandler.getHardwareMap(getOpModeManager()));
		} catch (Throwable e) {
			sneakyThrow(e);
		}

		this.driverHubTelemetry = new DriverHubTelemetry(hardwareOrchestrator);

		hardwareOrchestrator.subscribe("opmode/init", String.class, getOpModeManager()::initActiveOpMode);
		hardwareOrchestrator.subscribe("opmode/start", Object.class, __ -> getOpModeManager().startActiveOpMode());

		INSTANCE = this;
		hardwareOrchestrator.hardwareProvider.disarm();
	}

	public void onOpModePreInit(OpMode opMode) {
		if (opMode instanceof OpModeManagerImpl.DefaultOpMode)
			return;

		hardwareOrchestrator.hardwareProvider.arm();
		hardwareOrchestrator.startNode("telemetry", driverHubTelemetry);

		var gamepads = ftcEventLoopHandler.getEventLoopManager().getOpModeGamepads();

		GamepadAdaptor.hookGamepad(hardwareOrchestrator, gamepads[0], "gamepad1");
		GamepadAdaptor.hookGamepad(hardwareOrchestrator, gamepads[1], "gamepad2");

		driverHubTelemetry.clear();
	}

	public void onOpModePreStart(OpMode opMode) {

	}

	public void onOpModePostStop(OpMode opMode) {
		hardwareOrchestrator.hardwareProvider.disarm();
		hardwareOrchestrator.stopNode(driverHubTelemetry);
		synchronized (hardwareOrchestrator.exitLock) {
			hardwareOrchestrator.exitLock.notifyAll();
		}
	}

	@Override
	public void teardown() throws RobotCoreException, InterruptedException {
		try {
			super.teardown();
		} catch (Exception e) {
			e.printStackTrace();
		}

		hardwareOrchestrator.close();
	}

	private void startOrchestrator() {
		hardwareOrchestrator.close();
		try {
			var result = new HardwareOrchestrator(
				new RobotDetails("Physical Robot", random.nextLong()),
				hardwareOrchestrator.debugMode, hardwareOrchestrator.bridge != null,
				INSTANCE.ftcEventLoopHandler.getHardwareMap(INSTANCE.getOpModeManager())
			);
			hardwareOrchestrator = result;
			this.driverHubTelemetry = new DriverHubTelemetry(hardwareOrchestrator);
			hardwareOrchestrator.startNode("telemetry", driverHubTelemetry);

			hardwareInitialised = false;
		} catch (RobotCoreException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	public HardwareOrchestrator getHardwareOrchestrator() {
		return hardwareOrchestrator;
	}

	@OpModeRegistrar
	public static void registerOpMode(OpModeManager manager) {
		manager.register("Start network bridge", OpModeUtil.ofRunnable(() -> {
			if (INSTANCE == null)
				return;

			INSTANCE.hardwareOrchestrator.startBridge();
		}));

		manager.register("Restart orchestrator", OpModeUtil.ofRunnable(() -> {
			if (INSTANCE == null)
				return;

			INSTANCE.startOrchestrator();
		}));
	}
}
