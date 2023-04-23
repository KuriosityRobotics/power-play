package com.kuriosityrobotics.powerplay.debug;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryTreeTest {
	@Test
	void testEmpty() {
		assertEquals("", TelemetryTree.format(Collections.emptyList()));
	}

	@Test
	void testSingle() {
		List<TopicData> input = new ArrayList<>();
		input.add(new TopicData("foo", "bar"));
		System.out.println(TelemetryTree.format(input));
		assertEquals("|- foo = bar", TelemetryTree.format(input));
	}

	@Test
	void testBig() {
		List<TopicData> input = new ArrayList<>();
		input.add(new TopicData("encoder/1/velocity", 1.1));
		input.add(new TopicData("encoder/2/velocity", 3.3));
		input.add(new TopicData("encoder/3/velocity", 5.5));
		input.add(new TopicData("encoder/1/position", 2.2));
		input.add(new TopicData("encoder/2/position", 4.4));
		input.add(new TopicData("encoder/3/position", 6.6));
		input.add(new TopicData("started", "true"));
		input.add(new TopicData("localisation/a/baa/a/mm/cafsd", "dummy"));
		input.add(new TopicData("localisation/a/baa/a/mm/d", "dummy"));
		input.add(new TopicData("localisation/ba/b", "dummy"));

		assertEquals("|- localisation\n" +
			"|--- a/baa/a/mm\n" +
			"  --------|--- d = dummy\n" +
			"  --------|--- cafsd = dummy\n" +
			"|--- ba/b = dummy\n" +
			"|- started = true\n" +
			"|- encoder\n" +
			"|--- 1\n" +
			"  |--- position = 2.2\n" +
			"  |--- velocity = 1.1\n" +
			"|--- 2\n" +
			"  |--- position = 4.4\n" +
			"  |--- velocity = 3.3\n" +
			"|--- 3\n" +
			"  |--- position = 6.6\n" +
			"  |--- velocity = 5.5", TelemetryTree.format(input));
	}
}