package com.kuriosityrobotics.powerplay.pubsub;

import static com.kuriosityrobotics.powerplay.pubsub.OrchestratorImpl.sneakyThrow;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import java.util.concurrent.Callable;

public class OpModeUtil {
	private OpModeUtil() {}

	public static OpMode ofRunnable(Runnable c) {
		return new LinearOpMode() {
			@Override
			public void runOpMode() throws InterruptedException {
				try {
					c.run();
				} catch (Exception e) {
					sneakyThrow(e);
				}
			}
		};
	}
}
