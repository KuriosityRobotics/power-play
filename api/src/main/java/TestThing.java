import com.kuriosityrobotics.powerplay.debug.StdoutTopicLogger;
import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunnableAction;
import com.kuriosityrobotics.powerplay.pubsub.annotation.SubscribedTo;

import java.util.concurrent.atomic.AtomicInteger;

public class TestThing extends Node {
	private static final int NUM_EXECUTIONS = 100000;
	private final AtomicInteger executions = new AtomicInteger(0);

	protected TestThing(Orchestrator orchestrator) {
		super(orchestrator);
	}

	public static void main(String[] args) {
		var orchestrator = Orchestrator.create("testrobot");
		orchestrator.startBridge();
		orchestrator.startNode("log", new StdoutTopicLogger(orchestrator));
		var thing = new TestThing(orchestrator);
		orchestrator.startNode("test", thing);

		//		var start = System.nanoTime();
		//		for (int i = 0; i < NUM_EXECUTIONS; i++) orchestrator.dispatch("number", i);
		//
		//		while (orchestrator.queueNotEmpty()) Thread.onSpinWait();
		//		System.out.println((System.nanoTime() - start) / NUM_EXECUTIONS / 1000);
	}

	@SubscribedTo(topic = "number")
	public void printNumber(int number) {
		//        System.out.println("number: " + number);
	}

	@SubscribedTo(topic = "number")
	public void numberToAlphabet(int number) {
		orchestrator.dispatch(
			"alphabetCharacter",
			(char) (number + 'a')
		);
	}

	@SubscribedTo(topic = "alphabetCharacter")
	public void toUppercase(char number) {
		orchestrator.dispatch(
			"uppercaseCharacter",
			Character.toUpperCase(number)
		);
	}

	@SubscribedTo(topic = "uppercaseCharacter")
	public void printUppercase(char uppercase) {
		executions.incrementAndGet();
	}

	@RunnableAction(actionName = "test/funnyAction")
	void funnyAction() throws InterruptedException {
		Thread.sleep(3000);
		System.out.println(System.currentTimeMillis());
	}

	void funnyActionTest() {
		System.out.println(System.currentTimeMillis());
		orchestrator.startActionAsync("test/funnyAction");
	}
}
