package com.kuriosityrobotics.powerplay.pubsub;

public interface AsyncBlocker extends Runnable {
	@Override
	default void run() {
		while (!isDone()) Thread.yield();
	}

	boolean isDone();
}
