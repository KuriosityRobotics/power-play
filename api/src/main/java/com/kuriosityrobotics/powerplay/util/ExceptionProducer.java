package com.kuriosityrobotics.powerplay.util;

@FunctionalInterface
public interface ExceptionProducer<T> {
	T produce() throws Throwable;
}
