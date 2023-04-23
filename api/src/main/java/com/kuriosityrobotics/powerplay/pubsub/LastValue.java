package com.kuriosityrobotics.powerplay.pubsub;

/**
 * A <code>Subscription</code> that stores the last value published to the topic. Can be used, for
 * example if you just want to always have the latest <code>LocalisationDatum</code> published to
 * the <code>com.kuriosityrobotics.powerplay.localisation</code> topic on hand.
 *
 * @param <T>
 */
public abstract class LastValue<T> {
	protected T defaultValue;

	public abstract T getValue();

	public void setDefaultValue(T value) {
		defaultValue = value;
	}
}