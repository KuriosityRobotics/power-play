//package com.kuriosityrobotics.powerplay.pubsub;
//
//import com.kuriosityrobotics.powerplay.pubsub.annotation.RunnableAction;
//import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
//import com.kuriosityrobotics.powerplay.util.Duration;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.RepeatedTest;
//
//public class OrchestratorActionsTest {
//	private static final RobotDetails ROBOT_DETAILS = new RobotDetails("test", 0);
//	private Orchestrator orchestrator;
//	private boolean canClose = false;
//
//	@BeforeEach
//	void setUp() {
//		orchestrator = Orchestrator.createTest(ROBOT_DETAILS, false);
//		orchestrator.setBlockingDispatch(true);
//	}
//
//	@RepeatedTest(10)
//	public void testAction() {
//		canClose = false;
//		long startTime = System.currentTimeMillis();
//		orchestrator.startNode("test", new Node(orchestrator) {
//			public NodeTimer action1() {
//				return interruptibleTimer().delayThenRun(Duration.ofMillis(3000), () -> {
//					long endTime = System.currentTimeMillis();
//					System.out.println("end: " + endTime);
//					System.out.println("time: " + (endTime - startTime));
//					assertThat(endTime - startTime - 3000 < 150);
//					canClose = true;
//				});
//			}
//
//			@RunnableAction(actionName = "testAction2")
//			public NodeTimer action2() {
//				return interruptibleTimer().delayThenCompose(Duration.ofMillis(100), this::action1);
//			}
//		});
//		System.out.println("start: " + System.currentTimeMillis());
//		orchestrator.startActionAsync("testAction2");
//	}
//
//	@AfterEach
//	void tearDown() throws InterruptedException {
//		while (!canClose) {
//			Thread.sleep(1); // lol dw about it
//		}
//
//		// only close orchestrator when test is done
//		orchestrator.close();
//	}
//}
