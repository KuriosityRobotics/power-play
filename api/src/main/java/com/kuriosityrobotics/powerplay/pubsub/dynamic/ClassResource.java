package com.kuriosityrobotics.powerplay.pubsub.dynamic;

import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;

import java.io.Serializable;

@Hidden
public class ClassResource implements Serializable {
	private final ClassRequest request;
	private final byte[] bytes;

	public ClassResource(ClassRequest request, byte[] bytes) {
		this.request = request;
		this.bytes = bytes;
	}

	public ClassRequest request() {
		return request;
	}

	public String className() {
		return request.className();
	}

	public byte[] bytes() {
		return bytes;
	}
}
