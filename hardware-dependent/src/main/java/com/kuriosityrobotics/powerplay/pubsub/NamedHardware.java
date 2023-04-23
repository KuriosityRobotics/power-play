package com.kuriosityrobotics.powerplay.pubsub;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface NamedHardware {
   /**
	* The name of the hardware item in the config
	*/
   String value();
}
