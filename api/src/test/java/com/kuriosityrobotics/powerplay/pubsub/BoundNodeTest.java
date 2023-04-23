package com.kuriosityrobotics.powerplay.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kuriosityrobotics.powerplay.pubsub.annotation.LastMessagePublished;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class BoundNodeTest {
	private AtomicInteger number;
	private AtomicInteger alphabetCharacter;
	private AtomicInteger uppercaseCharacter;

   @SuppressWarnings("FieldMayBeFinal")
   private class TestNode extends Node {
		@LastMessagePublished(topic = "number")
		private volatile int lastNumber = -1;

		public int lastNumber() {
			return lastNumber;
		}

		protected TestNode(Orchestrator orchestrator) {
			super(orchestrator);
		}

		@SubscribedTo(topic = "number")
		public void setNumber(int number_) {
			number.set(number_);
		}

		@SubscribedTo(topic = "number")
		public void toAlphabetCharacter(
				int alphabetCharacter_, String topicName, RobotDetails origin) {
			assertEquals("number", topicName);
			assertEquals(orchestrator.robotDetails(), origin);

			alphabetCharacter.set((alphabetCharacter_ % 26) + 'a');

			orchestrator.dispatch ("alphabetCharacter", (char) ((alphabetCharacter_ % 26) + 'a'));
		}

		@SubscribedTo(topic = "alphabetCharacter")
		public void toUppercaseCharacter(Character alphabetCharacter_) {
			uppercaseCharacter.set(Character.toUpperCase(alphabetCharacter_));
			orchestrator.dispatch("uppercaseCharacter", Character.toUpperCase(alphabetCharacter_));
		}
	}

	private TestNode testNode;
	private Orchestrator orchestrator;

	@BeforeEach
	void setUp() {
		number = new AtomicInteger();
		alphabetCharacter = new AtomicInteger();
		uppercaseCharacter = new AtomicInteger();

		orchestrator = Orchestrator.create("testrobot");
		testNode = new TestNode(orchestrator);
		orchestrator.startNode("test", testNode);
	}

	@Test
	void test() throws InterruptedException {
		for (int i = 0; i < 20; i++) {
			orchestrator.dispatch("number", i);
			Thread.sleep(100);

			assertEquals(i, testNode.lastNumber());
			assertEquals(i, number.get());
			assertEquals((i % 26) + 'a', alphabetCharacter.get());
			assertEquals((i % 26) + 'A', uppercaseCharacter.get());
		}
	}
}
