package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.*;
import com.qualcomm.robotcore.hardware.AnalogInput;

public interface HardwareProvider {
   LynxModule moduleFor(RobotConstants.LynxHub hub);

   DcMotorControllerEx motorControllerFor(RobotConstants.LynxHub hub);

   AnalogInputController analogInputControllerFor(RobotConstants.LynxHub hub);

   ServoControllerEx servoControllerFor(RobotConstants.LynxHub hub);

   DcMotorEx motor(RobotConstants.LynxHub hub, int portNumber, DcMotorSimple.Direction direction);

   ServoImplEx servo(RobotConstants.LynxHub hub, int portNumber);

   AnalogInput analogInput(RobotConstants.LynxHub hub, int channel);

   <T extends HardwareDevice> T byName(Class<? extends T> classOrInterface, String deviceName);
   <T extends HardwareDevice> T byName(String deviceName);

   void arm();
   void disarm();
}
