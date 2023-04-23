package com.kuriosityrobotics.powerplay.pubsub.bridge.message;

import java.io.Serializable;

public class NodeInfo implements Serializable {
	private final String nodeType;
	private final String nodeName;

	public NodeInfo(String nodeType, String nodeName) {
		this.nodeType = nodeType;
		this.nodeName = nodeName;
	}

	public String nodeType() {
		return nodeType;
	}

	public String nodeName() {
		return nodeName;
	}

	@Override
	public String toString() {
		return nodeName;
	}
}
