package com.kuriosityrobotics.powerplay.pubsub;

public interface LogInterface {
   /**
	* Sends a telemetry message to the error topic with the given message and throws a {@link
	* RuntimeException} with the given message if debug mode is enabled.
	*
	* @param msg the message
	*/
   void err(String msg);

   /**
	* Sends a telemetry message to the warning topic
	*
	* @param msg the message
	*/
   void warn(String msg);

   /**
	* Sends a telemetry message to the info topic
	*
	* @param msg the message
	*/
   void info(String msg);

   /**
	* Sends a telemetry message to the debug topic
	*
	* @param msg the message
	*/
   void debug(String msg);

   /**
	* Sends a telemetry message to the error topic, and throws a {@link RuntimeException} with the
	* given message if debug
	*
	* @param msg             the message
	* @param originatingNode the node from whom the message originated
	*/
   void err(String msg, Node originatingNode);

   /**
	* Sends an exception to the error topic, and throws a {@link RuntimeException} with the given
	* message if debug
	*
	* @param e               the exception
	* @param originatingNode the node from whom the message originated
	*/
   void err(Throwable e, Node originatingNode);

   /**
	* Sends an exception to the error topic, and throws a {@link RuntimeException} with the given
	* message if debugMode is true.
	*
	* @param e the exception
	*/
   void err(Throwable e);

   /**
	* Sends a telemetry message to the warning topic
	*
	* @param msg             the message
	* @param originatingNode the node from whom the message originated
	*/
   void warn(String msg, Node originatingNode);

   /**
	* Sends an exception to the warn topic
	*
	* @param e               the exception
	* @param originatingNode the node from whom the message originated
	*/
   void warn(Throwable e, Node originatingNode);

   /**
	* Sends a telemetry message to the info topic
	*
	* @param msg             the message
	* @param originatingNode the node from whom the message originated
	*/
   void info(String msg, Node originatingNode);

   /**
	* Sends a telemetry message to the debug topic
	*
	* @param msg             the message
	* @param originatingNode the node from whom the message originated
	*/
   void debug(String msg, Node originatingNode);

   /**
	* Sends an object to telemetry with the given name
	*
	* @param name the name of the object
	* @param obj  the object
	*/
   void telemetry(String name, Object obj);

   /**
	* Sends an object to telemetry with the given name
	*
	* @param name            the name of the object
	* @param obj             the object
	* @param originatingNode the node from whom the message originated
	*/
   void telemetry(String name, Object obj, Node originatingNode);

   /**
	* Asserts that the given condition is true. This is used for debugging.
	*
	* @param condition the condition to assert
	* @param message   the message to log if the condition is false
	* @return the condition
	*/
   boolean assertThat(boolean condition, String message);

   /**
	* Asserts that the given condition is true. This is used for debugging.
	*
	* @param condition the condition to assert
	* @return the condition
	*/
   boolean assertThat(boolean condition);
}
