package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Servomotor {
   RobotConstants.LynxHub hub();
   int port();
}
