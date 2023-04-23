package com.kuriosityrobotics.powerplay.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A lock that can be preempted by another thread.
 * If a lock is 'preempted', the holder of the lock will be interrupted.
 */
public class PreemptibleLock extends ReentrantLock {
	public PreemptibleLock() {
		super();
	}

	public PreemptibleLock(boolean fair) {
		super(fair);
	}


	private final ReentrantLock acquisitionLock = new ReentrantLock();
	/**
	 * Attempts to acquire the lock by interrupting the current holder if necessary.
	 */
	public void lockForcibly() throws InterruptedException {
		acquisitionLock.lockInterruptibly();
		try {
			if (isHeldByCurrentThread()) {
				return;
			}
			if (isLocked()) {
				new RuntimeException("interruptor").printStackTrace();
				try {
					getOwner().interrupt();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			}
			super.lockInterruptibly();
		} finally {
			acquisitionLock.unlock();
		}
	}

	@Override
	public void unlock() {
		try {
			super.unlock();
		} catch (IllegalMonitorStateException e) {
			// we were preempted;  ignore
		}
	}

	@Override
	public void lock() {
		throw new UnsupportedOperationException("Use lockForcibly()");
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		throw new UnsupportedOperationException("Use lockForcibly()");
	}
}
