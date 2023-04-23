package com.kuriosityrobotics.powerplay.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static java.lang.Math.*;

public final class StringUtils {
   private StringUtils() {
   }

   /**
	* converts the given object into a string that looks nice
	*
	* @param object the object to convert
	* @return a nice-looking String representation of the object
	* @see java.text.Format
	*/
   public static String toDisplayString(Object object) {
	  if (object instanceof Number) {
		  return sigFiggedString(((Number) object).doubleValue(), 3);
	  }else
		 return object.toString();
   }
   private static final DecimalFormat df = new DecimalFormat("0.####");
   public static String sigFiggedString(double x, int numSigFigs) {
	  var y = pow(10, (ceil(log10(abs(x))) - numSigFigs));
	  return df.format(y * round(x / y));
   }
}
