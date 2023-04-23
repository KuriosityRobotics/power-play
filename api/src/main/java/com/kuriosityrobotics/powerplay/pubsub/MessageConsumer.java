package com.kuriosityrobotics.powerplay.pubsub;

@FunctionalInterface
public interface MessageConsumer<A, B, C> {
	void accept(A a, B b, C c);
}
