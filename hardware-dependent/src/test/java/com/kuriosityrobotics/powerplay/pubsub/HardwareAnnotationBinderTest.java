package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.hardware.RobotConstants;
import com.qualcomm.hardware.lynx.*;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import org.junit.jupiter.api.Test;

import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.CONTROL_HUB;
import static com.kuriosityrobotics.powerplay.hardware.RobotConstants.LynxHub.EXPANSION_HUB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HardwareAnnotationBinderTest {
   private static LynxModule getLynxModule(LynxController controller) throws IllegalAccessException, NoSuchFieldException {
	  var field = LynxController.class.getDeclaredField("module");
	  field.setAccessible(true);
	  return (LynxModule) field.get(controller);
   }

   @Test
   void bind() throws NoSuchFieldException, IllegalAccessException {
	  class TestBindClass extends Node {
		 @MotorOrEncoder(hub = EXPANSION_HUB, port = 0)
		 DcMotor motor;
		 @Hub(CONTROL_HUB)
		 LynxModule hub;
		 @NamedHardware("test")
		 DcMotor namedMotor;
		 @Servomotor(hub = CONTROL_HUB, port = 1)
		 Servo servomotor;

		 TestBindClass(Orchestrator orchestrator) {
			super(orchestrator);
		 }
	  }

	  var hardwareMap = mock(HardwareMap.class);
	  var testMotor = mock(DcMotor.class);
	  when(hardwareMap.get("test")).thenReturn(testMotor);

	  var mockEH = mock(LynxModule.class);
	  var mockCH = mock(LynxModule.class);

	  when(hardwareMap.get(LynxModule.class, EXPANSION_HUB.hardwareName())).thenReturn(mockEH);
	  when(hardwareMap.get(LynxModule.class, CONTROL_HUB.hardwareName())).thenReturn(mockCH);

	  var hardwareProvider = spy(new HardwareProviderImpl(mock(Orchestrator.class), hardwareMap));

	  { // make sure the module of the controller is the same as the module of the hub
		 var moduleField = LynxController.class.getDeclaredField("module");
		 moduleField.setAccessible(true);

		 doAnswer(hub -> {
			var controller = hub.getArgument(0, RobotConstants.LynxHub.class);
			var mockDcMotorController = mock(LynxDcMotorController.class);
			moduleField.set(mockDcMotorController, controller == EXPANSION_HUB ? mockEH : mockCH);
			return mockDcMotorController;
		 }).when(hardwareProvider).motorControllerFor(any());

		 doAnswer(hub -> {
			var controller = hub.getArgument(0, RobotConstants.LynxHub.class);
			var mockServoController = mock(LynxServoController.class);
			moduleField.set(mockServoController, controller == EXPANSION_HUB ? mockEH : mockCH);
			return mockServoController;
		 }).when(hardwareProvider).servoControllerFor(any());

		 doAnswer(hub -> {
			var controller = hub.getArgument(0, RobotConstants.LynxHub.class);
			var mockAnalogInputController = mock(LynxAnalogInputController.class);
			moduleField.set(mockAnalogInputController, controller == EXPANSION_HUB ? mockEH : mockCH);
			return mockAnalogInputController;
		 }).when(hardwareProvider).analogInputControllerFor(any());
	  }

	  var test = new TestBindClass(mock(HardwareOrchestrator.class));
	  var binder = new HardwareAnnotationBinder(mock(LogInterface.class), hardwareProvider);

	  try (var motorConfig = mockStatic(MotorConfigurationType.class);
	  var servoConfig = mockStatic(ServoConfigurationType.class)) {
		 motorConfig.when(MotorConfigurationType::getUnspecifiedMotorType).thenReturn(new MotorConfigurationType());
		 servoConfig.when(ServoConfigurationType::getStandardServoType).thenReturn(new ServoConfigurationType());
		 binder.bind(test);
	  }

	  assertEquals(testMotor, test.namedMotor);

	  assertEquals(mockCH, test.hub);
	  assertEquals(0, test.motor.getPortNumber());
	  assertEquals(mockEH, getLynxModule((LynxDcMotorController) test.motor.getController()));

	  assertEquals(1, test.servomotor.getPortNumber());
	  assertEquals(mockCH, getLynxModule((LynxServoController) test.servomotor.getController()));
   }
}