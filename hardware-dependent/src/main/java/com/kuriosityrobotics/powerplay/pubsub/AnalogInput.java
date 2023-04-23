package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AnalogInput {
   LynxHub hub();
   int channel();
}
