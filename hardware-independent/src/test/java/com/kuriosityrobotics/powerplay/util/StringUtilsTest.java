package com.kuriosityrobotics.powerplay.util;

import static com.kuriosityrobotics.powerplay.util.StringUtils.sigFiggedString;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringUtilsTest {
   @Test
   void testSigFiggedString() {
	  assertEquals("1", sigFiggedString(1.00001, 1));
	  assertEquals("-1", sigFiggedString(-1.00001, 1));
	  assertEquals("100", sigFiggedString(123, 1));
	  assertEquals("1.1", sigFiggedString(1.1, 2));
	  assertEquals("1", sigFiggedString(1.01, 2));
	  assertEquals("1", sigFiggedString(1.01, 1));
   }
}