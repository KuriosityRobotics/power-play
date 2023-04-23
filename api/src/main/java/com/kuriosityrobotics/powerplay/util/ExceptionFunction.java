package com.kuriosityrobotics.powerplay.util;

@FunctionalInterface
public interface ExceptionFunction<T, U> {
	U execute(T t) throws Throwable;
}
