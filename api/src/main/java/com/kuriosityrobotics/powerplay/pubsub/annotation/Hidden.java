package com.kuriosityrobotics.powerplay.pubsub.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Don't tell other devices on the network about this node. */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface Hidden {}
