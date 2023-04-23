package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants.*;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MotorOrEncoder {
   LynxHub hub();
   int port();
   DcMotorSimple.Direction direction() default DcMotorSimple.Direction.FORWARD;
}
