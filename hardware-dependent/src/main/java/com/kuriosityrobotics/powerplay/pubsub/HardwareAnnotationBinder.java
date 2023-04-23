package com.kuriosityrobotics.powerplay.pubsub;

import com.qualcomm.robotcore.hardware.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class HardwareAnnotationBinder {
   private final LogInterface logInterface;
   private final HardwareProvider hardwareProvider;

   public HardwareAnnotationBinder(
		   LogInterface logInterface, 
		   HardwareProvider hardwareProvider
   ) {
	  this.logInterface = logInterface;
	  this.hardwareProvider = hardwareProvider;
   }

   void bind(Node node) throws IllegalAccessException {
	  for (Field field : node.getClass().getDeclaredFields()) {
		 bindNamedHardware(node, field);
		 bindMotorOrEncoder(node, field);
		 bindAnalogInput(node, field);
		 bindServo(node, field);
		 bindHub(node, field);
	  }
   }


   private void bindNamedHardware(Node node, Field field) throws IllegalAccessException {
	  var namedHardware = field.getAnnotation(NamedHardware.class);
	  if (namedHardware == null) return;

	  field.setAccessible(true);

	  if ((field.getModifiers() & Modifier.FINAL) != 0) {
		 logInterface.err(
				 "NamedHardware fields must not be final:  " + field.getName() + " in " + node.getClass().getSimpleName() + ".  If the IDE is whinging, just slape a @SuppressWarnings(\"FieldMayBeFinal\") before the class declaration.");
		 return;
	  }
	  field.set(node, hardwareProvider.byName(namedHardware.value()));
   }

   private void bindMotorOrEncoder(Node node, Field field) throws IllegalAccessException {
	  var motorOrEncoder = field.getAnnotation(MotorOrEncoder.class);
	  if (motorOrEncoder == null) return;

	  field.setAccessible(true);

	  if ((field.getModifiers() & Modifier.FINAL) != 0) {
		 logInterface.err(
				 "MotorOrEncoder fields must not be final:  " + field.getName() + " in " + node.getClass().getSimpleName() + ".  If the IDE is whinging, just slape a @SuppressWarnings(\"FieldMayBeFinal\") before the class declaration.");
		 return;
	  }
	  field.set(node, hardwareProvider.motor(motorOrEncoder.hub(), motorOrEncoder.port(), motorOrEncoder.direction()));
   }

   private void bindAnalogInput(Node node, Field field) throws IllegalAccessException {
	  var motorOrEncoder = field.getAnnotation(AnalogInput.class);
	  if (motorOrEncoder == null) return;

	  field.setAccessible(true);

	  if ((field.getModifiers() & Modifier.FINAL) != 0) {
		 logInterface.err(
				 "AnalogInput fields must not be final:  " + field.getName() + " in " + node.getClass().getSimpleName() + ".  If the IDE is whinging, just slape a @SuppressWarnings(\"FieldMayBeFinal\") before the class declaration.");
		 return;
	  }
	  field.set(node, hardwareProvider.analogInput(motorOrEncoder.hub(), motorOrEncoder.channel()));
   }

   private void bindServo(Node node, Field field) throws IllegalAccessException {
	  var servo = field.getAnnotation(Servomotor.class);
	  if (servo == null) return;

	  field.setAccessible(true);

	  if ((field.getModifiers() & Modifier.FINAL) != 0) {
		 logInterface.err(
				 "Servo fields must not be final:  " + field.getName() + " in " + node.getClass().getSimpleName() + ".  If the IDE is whinging, just slape a @SuppressWarnings(\"FieldMayBeFinal\") before the class declaration.");
		 return;
	  }
	  field.set(node, hardwareProvider.servo(servo.hub(), servo.port()));
   }

   private void bindHub(Node node, Field field) throws IllegalAccessException {
	  var hub = field.getAnnotation(Hub.class);
	  if (hub == null) return;

	  field.setAccessible(true);

	  if ((field.getModifiers() & Modifier.FINAL) != 0) {
		 logInterface.err(
				 "Hub fields must not be final:  " + field.getName() + " in " + node.getClass().getSimpleName() + ".  If the IDE is whinging, just slape a @SuppressWarnings(\"FieldMayBeFinal\") before the class declaration.");
		 return;
	  }
	  field.set(node, hardwareProvider.moduleFor(hub.value()));
   }
}
