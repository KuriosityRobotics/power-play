package com.kuriosityrobotics.powerplay.hardware;

import static com.kuriosityrobotics.powerplay.auto.AutoUtils.sneakyThrow;

import com.kuriosityrobotics.powerplay.util.Duration;
import com.kuriosityrobotics.powerplay.util.Instant;
import com.kuriosityrobotics.powerplay.util.PreemptibleLock;

import java.util.concurrent.TimeoutException;

public abstract class LinearMotorControl {
	protected final PreemptibleLock lock = new PreemptibleLock();
	private final Duration timeout;

	protected LinearMotorControl() {
		this.timeout = Duration.ofSeconds(5);
	}

	/**
	 * @param timeout The maximum time to wait for the motor to reach its target position.  After this time is elapsed, execution will continue, regardless of whether the encoder has reached its target position.
	 */
	protected LinearMotorControl(Duration timeout) {
		this.timeout = timeout;
	}

	protected void goToPosition(double position) throws InterruptedException, HardwareException {
		lock.lockForcibly();
		try {
			setTargetPositionMetres0(position);

			var startTime = Instant.now();
			while (!Thread.interrupted() && Instant.now().since(startTime).isLessThan(timeout)) {
				throwIfOverCurrent();
				if (isBusy()) {
					Thread.sleep(30);
				} else {
					break;
				}
			}

			if (Instant.now().since(startTime).isGreaterThan(timeout))
				( new TimeoutException("Timed out:  did not finish within " + timeout.toSeconds() + " seconds.")).printStackTrace();

			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		} finally {
			lock.unlock();
		}
	}


	protected abstract boolean isBusy();

	protected abstract double getCurrentPositionMetres();

	protected abstract void setTargetPositionMetres0(double position);
	protected void throwIfOverCurrent() throws OverCurrentFault {
		if (isOverCurrent())
			System.out.println("over current");
			throw new OverCurrentFault(getClass().getSimpleName());
	}

	protected abstract boolean isOverCurrent();
}
