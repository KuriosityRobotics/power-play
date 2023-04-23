package com.kuriosityrobotics.powerplay.util;

@FunctionalInterface
public interface ExceptionRunnable {
	void run() throws Throwable;
}
