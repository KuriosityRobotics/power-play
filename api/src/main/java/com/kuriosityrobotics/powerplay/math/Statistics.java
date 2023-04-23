package com.kuriosityrobotics.powerplay.math;

import java.util.Collection;
import java.util.OptionalDouble;
import java.util.stream.Stream;

public final class Statistics {
	/** Finds the mean of a collection */
	public static OptionalDouble mean(Stream<? extends Number> numbers) {
		return numbers.mapToDouble(Number::doubleValue).average();
	}

	public static OptionalDouble mean(Collection<? extends Number> numbers) {
		return mean(numbers.stream());
	}
}
