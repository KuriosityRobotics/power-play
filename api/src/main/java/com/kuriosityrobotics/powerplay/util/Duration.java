package com.kuriosityrobotics.powerplay.util;

public class Duration {
	private final long timeHolder;
	private static final long ONE_MILLION = 1_000_000;
	private static final long ONE_BILLION = 1_000 * ONE_MILLION;

	private Duration(long time) {
		timeHolder = time;
	}

	public static Duration ofNanos(long nanos) {
		return new Duration(nanos);
	}

	public static Duration ofMillis(long millis) {
		return new Duration(millis * ONE_MILLION);
	}

	public static Duration ofSeconds(double seconds) {
		return ofNanos((long)(seconds * ONE_BILLION));
	}

	public Duration negated() {
		return new Duration(-timeHolder);
	}

	public long toNanos() {
		return timeHolder;
	}

	public long toMillis() {
		return (timeHolder - (timeHolder % ONE_MILLION)) / ONE_MILLION;
	}

	public double toSeconds() {
		return timeHolder / (double) ONE_BILLION;
	}

	public boolean isGreaterThan(Duration duration) {
		return timeHolder > duration.timeHolder;
	}

	public boolean isLessThan(Duration duration) {
		return timeHolder < duration.timeHolder;
	}
}
