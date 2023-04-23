package com.kuriosityrobotics.powerplay.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ConcurrencyUtils {
	public static void sleep(Duration duration) throws InterruptedException {
		Thread.sleep(duration.toMillis(), (int) (duration.toNanos() % 1e6));
	}
}
