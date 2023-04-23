package com.kuriosityrobotics.powerplay.pubsub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionsTest {
	private static final long TIMEOUT_MS = 20;
	private Orchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator = Orchestrator.createTest("test", false);
	}

	@RepeatedTest(10)
	void childInterruptsParent() {
		var action = orchestrator.startActionAsync(() -> {
			var child = orchestrator.startActionAsync(() -> {
				throw new InterruptedException();
			});
			while (!child.isDone()) ; // wait for child to finish
			// wait up to TIMEOUT_MS ms for parent to be interrupted
			boolean wasInterrupted = false;
			try {
				Thread.sleep(TIMEOUT_MS);
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
			assertTrue(wasInterrupted);
		});

		action.join();
	}

	@RepeatedTest(10)
	void parentInterruptsChild() throws InterruptedException {
		AtomicReference<CompletableFuture<?>> child = new AtomicReference<>();

		new Thread(() -> {
			var parent = orchestrator.startActionAsync(() -> {
				CountDownLatch barrier = new CountDownLatch(2);
				child.set(orchestrator.startActionAsync(() -> {
					barrier.countDown();
					// wait up to TIMEOUT_MS ms for child to be interrupted
					boolean wasInterrupted = false;
					try {
						barrier.await();
						Thread.sleep(TIMEOUT_MS);
					} catch (InterruptedException e) {
						wasInterrupted = true;
					}
					assertTrue(wasInterrupted);
				}));
				barrier.countDown();
				barrier.await();

				throw new InterruptedException();
			});
		}).start();

		// wait for child to start
		while (child.get() == null) ;
		child.get().join();
	}

	@RepeatedTest(10)
	void siblingInterruptsSibling() throws InterruptedException {
		var barrier = new CountDownLatch(2);

		orchestrator.startActionAsync(() -> {
			var sibling1 = orchestrator.startActionAsync(() -> {
				barrier.countDown();
				// wait up to TIMEOUT_MS ms for sibling to be interrupted
				boolean wasInterrupted = false;
				try {
					barrier.await();
					Thread.sleep(TIMEOUT_MS);
				} catch (InterruptedException e) {
					wasInterrupted = true;
				}
				assertTrue(wasInterrupted);
			});

			var sibling2 = orchestrator.startActionAsync(() -> {
				barrier.countDown();
				barrier.await();
				throw new InterruptedException();
			});

			sibling1.join();
		}).join();
	}

	@RepeatedTest(10)
	void grandchildInterruptsParent() throws InterruptedException {
		var parent = orchestrator.startActionAsync(() -> {
			var barrier = new CountDownLatch(2);
			var child = orchestrator.startActionAsync(() -> {
				var grandchild = orchestrator.startActionAsync(() -> {
					barrier.countDown();
					barrier.await();
					throw new InterruptedException();
				});
				while (!grandchild.isDone()) ; // wait for grandchild to finish
			});

			barrier.countDown();
			boolean wasInterrupted = false;
			try {
				barrier.await();
				Thread.sleep(TIMEOUT_MS);
			} catch (InterruptedException e) {
				wasInterrupted = true;
			}
			assertTrue(wasInterrupted);
		});
		parent.join();
	}

	@RepeatedTest(10)
	void parentInterruptsGrandchild() {
		AtomicReference<CompletableFuture<?>> grandchild = new AtomicReference<>();

		new Thread(() -> {
			var parent = orchestrator.startActionAsync(() -> {
				var barrier = new CountDownLatch(2);
				orchestrator.startActionAsync(() -> {
					grandchild.set(orchestrator.startActionAsync(() -> {
						barrier.countDown();
						// wait up to 20 ms for grandchild to be interrupted
						boolean wasInterrupted = false;
						try {
							barrier.await();
							Thread.sleep(TIMEOUT_MS);
						} catch (InterruptedException e) {
							wasInterrupted = true;
						}
						assertTrue(wasInterrupted);
					}));
					while (!grandchild.get().isDone()) ; // wait for grandchild to finish
				});

				barrier.countDown();
				barrier.await();

				throw new InterruptedException();
			});
		}).start();

		// wait for grandchild to start
		while (grandchild.get() == null) ;
		grandchild.get().join();
	}

	@RepeatedTest(10)
	void nephewInterruptsUncle() throws InterruptedException {
		var barrier = new CountDownLatch(2);

		orchestrator.startActionAsync(() -> {
			var uncle = orchestrator.startActionAsync(() -> {
				barrier.countDown();

				// wait up to 20 ms for uncle to be interrupted
				boolean wasInterrupted = false;
				try {
					barrier.await();
					Thread.sleep(TIMEOUT_MS);
				} catch (InterruptedException e) {
					wasInterrupted = true;
				}
				assertTrue(wasInterrupted);
			});

			var parent = orchestrator.startActionAsync(() -> {
				var nephew = orchestrator.startActionAsync(() -> {
					barrier.countDown();
					barrier.await();
					throw new InterruptedException();
				});
				while (!nephew.isDone()) ; // wait for nephew to finish
				while (!uncle.isDone()) ; // wait for uncle to finish
			});

			while (!parent.isDone()) ; // wait for parent to finish
		}).join();
	}
}
