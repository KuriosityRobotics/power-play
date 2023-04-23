package com.kuriosityrobotics.powerplay.pubsub.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Repeatable(SubscribedTos.class)
public @interface SubscribedTo {
	java.lang.String topic();

	boolean onlyLocal() default false;

	boolean isPattern() default false;
}
