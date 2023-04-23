package com.kuriosityrobotics.powerplay.auto;

import com.kuriosityrobotics.powerplay.io.DepositTarget;
import com.kuriosityrobotics.powerplay.io.PickupTarget;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunnableAction;
import com.kuriosityrobotics.powerplay.util.Duration;
import com.kuriosityrobotics.powerplay.util.Instant;

import java.util.concurrent.ExecutionException;

public class AutoUtils {
	private final Orchestrator orchestrator;

	public AutoUtils(Orchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	public static <T extends Throwable> T sneakyThrow(Throwable t) throws T {
		throw (T) t;
	}


	public void depositPreload(boolean beginExtending) throws InterruptedException, ExecutionException {
		awaitStablePosition();
		System.out.println("MPC finished");
		var deposit = orchestrator.startActionAsync("io/deposit");

		awaitStablePosition();

		if (beginExtending) {
			orchestrator.dispatchSynchronous("io/intake/target", PickupTarget.fromStackHeight(5));
			orchestrator.startActionAsync("io/intake/farPickup"); // begin extending early
		}

		deposit.get();
	}

	public void doAutoCycles(int numCycles, int startingHeight, boolean awaitStablePosition) throws InterruptedException, ExecutionException {
		if (numCycles > startingHeight)
			throw new IllegalArgumentException("numCycles must be less than or equal to startingHeight");

		for (int i = startingHeight; i > startingHeight - numCycles; i--) {
			// extend the intake
			if (awaitStablePosition)
				awaitStablePosition();
			orchestrator.dispatchSynchronous("io/intake/target", PickupTarget.fromStackHeight(i));
			if (awaitStablePosition)
				awaitStablePosition();
			orchestrator.startActionAsync("io/intake/farPickup").get();
			orchestrator.startActionAsync("io/transfer").get();

			if (i > (startingHeight - numCycles + 1)) // everything except the last cycle
				orchestrator.startActionAsync("io/intake/farPickup"); // no get;  just extend early

			deposit();
		}
	}

	public void awaitStablePosition() throws InterruptedException, ExecutionException {
		orchestrator.startActionAsync("mpc/awaitPathEnd").get();
	}

	public void deposit() throws InterruptedException {
		try {
			orchestrator.startActionAsync("io/deposit").get();
		} catch (ExecutionException e) {
			sneakyThrow(e.getCause());
		}
	}

	public void setDepositTarget(DepositTarget target) {
		orchestrator.dispatchSynchronous("io/outtake/target", target);
	}

}

