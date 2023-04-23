package com.kuriosityrobotics.powerplay.pubsub;

import android.content.Context;

import com.qualcomm.ftccommon.FtcEventLoop;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop;

import java.util.concurrent.ExecutionException;

public abstract class OrchestratedOpMode extends LinearOpMode {
	protected static OrchestratorBoundEventLoop eventLoop;

	@Override
	public final void runOpMode() {
		if (eventLoop != null) {
			var orchestrator = eventLoop.getHardwareOrchestrator();

			synchronized (orchestrator.exitLock) {
				try {
					if (eventLoop != null) {
						eventLoop.onOpModePreInit(this);
					}
					initOpMode(orchestrator);
					orchestrator.dispatch("hardware/arm", "");

					telemetry.setMsTransmissionInterval(1 << 32 - 1);

					waitForStart();

					if (shouldStop())
						return;

					if (eventLoop != null) {
						eventLoop.onOpModePreStart(this);
					}

					runOpMode(orchestrator);
					orchestrator.exitLock.wait();
				} catch (InterruptedException ignored) {
				} catch (ExecutionException e) {
					e.printStackTrace();
				} finally {
					cleanupOpMode(orchestrator);
					if (eventLoop != null) {
						eventLoop.onOpModePostStop(this);
					}
				}
			}

		} else {
			try (var orchestrator = HardwareOrchestrator.create(hardwareMap)) {
				GamepadAdaptor.hookGamepad(orchestrator, gamepad1, "gamepad1");
				GamepadAdaptor.hookGamepad(orchestrator, gamepad2, "gamepad2");

				try {
					synchronized (orchestrator.exitLock) {
						if (eventLoop != null) {
							eventLoop.onOpModePreInit(this);
						}
						initOpMode(orchestrator);
						orchestrator.dispatch("hardware/arm", "");

						waitForStart();

						if (shouldStop())
							return;

						if (eventLoop != null) {
							eventLoop.onOpModePreStart(this);
						}

						runOpMode(orchestrator);
						orchestrator.exitLock.wait();
					}
				} catch (InterruptedException ignored) {
				} catch (ExecutionException e) {
					e.printStackTrace();
				} finally {
					orchestrator.stopNode("gamepad1");
					orchestrator.stopNode("gamepad2");
					cleanupOpMode(orchestrator);
					if (eventLoop != null) {
						eventLoop.onOpModePostStop(this);
					}
				}
			}
		}
	}

	protected void initOpMode(HardwareOrchestrator orchestrator) {
	}

	;

	protected void runOpMode(HardwareOrchestrator orchestrator) throws InterruptedException, ExecutionException {
	}

	;

	protected void cleanupOpMode(HardwareOrchestrator orchestrator) {
	}

	;

	@OnCreateEventLoop
	public static void onCreateEventLoop(Context ctx, FtcEventLoop eventLoop) {
		if (eventLoop instanceof OrchestratorBoundEventLoop)
			OrchestratedOpMode.eventLoop = ((OrchestratorBoundEventLoop) eventLoop);
	}

	public boolean shouldStop() {
		return isStopRequested() || !opModeIsActive();
	}
}
