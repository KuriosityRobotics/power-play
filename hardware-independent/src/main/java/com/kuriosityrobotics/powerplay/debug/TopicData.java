package com.kuriosityrobotics.powerplay.debug;

import static java.util.Objects.requireNonNull;

public class TopicData {
	/**
	 * take a guess lol
	 */
	public String topicName;
	
	/**
	 * last data published to this topic
	 */
	public Object data;

	/**
	 * the (fixed) number of lines that this topic will be allowed on the Driver Hub
	 */
	public int numberOfLinesOnDH;

	@Override
	public boolean equals(Object o){
		if(o instanceof TopicData){
			return ((TopicData) o).topicName.equals(topicName);
		}
		return false;
	}

	@Override
	public String toString(){
		return topicName;
	}

	public TopicData(String name, Object data) {
		requireNonNull(name);

		this.data = data;
		this.topicName = name;
	}

	public void calculateLines(int width) {
		// lol do something heheheha
		this.numberOfLinesOnDH = 2;
	}
}