package com.kuriosityrobotics.powerplay.pubsub;

import static com.kuriosityrobotics.powerplay.pubsub.OrchestratorImpl.sneakyThrow;

import com.google.common.base.Throwables;
import com.kuriosityrobotics.powerplay.hardware.HardwareException;

import org.jetbrains.annotations.NotNull;
import org.ojalgo.machine.Hardware;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ActionExecutor extends ThreadPoolExecutor {
	public ActionExecutor(ThreadFactory threadFactory) {
		super(
			0,
			Integer.MAX_VALUE,
			60L,
			TimeUnit.SECONDS,
			new SynchronousQueue<>(),
			threadFactory
		);
	}

	static class ActionRunner {
		private ActionRunner parent;
		private final Thread thread;
		private final Set<ActionRunner> children;

		private final ReentrantLock lock = new ReentrantLock();

		ActionRunner(ActionRunner parent, Thread thread) {
			this.parent = parent;
			this.thread = thread;
			this.children = ConcurrentHashMap.newKeySet();
		}

		private void interruptHierarchy() throws InterruptedException {
			// go up the tree to find the root
			ActionRunner root = this;
			lock.lockInterruptibly();
			try {
				while (root.parent != null) {
					root = root.parent;
				}

				// interrupt the root
				root.interrupt();
			} finally {
				lock.unlock();
			}
		}

		void interrupt() {
			if (lock.tryLock()) {
				try {
					thread.interrupt();
					for (ActionRunner child : children) {
						System.out.println("interrupting child");
						new RuntimeException("interrupting").printStackTrace();
						child.interrupt();
					}
					children.clear();
				} finally {
					lock.unlock();
				}
			}
		}

		void complete() {
			if (lock.tryLock()) {
				try {
					for (ActionRunner child : children) {
						child.complete();
					}
					children.clear();
					if (parent != null) {
						parent.children.remove(this);
						parent = null;
					}
				} finally {
					lock.unlock();
				}
			}
		}
	}

	private final ThreadLocal<ActionRunner> runner = new ThreadLocal<>();

	@Override
	public void execute(@NotNull Runnable command) {
		var parentRunner = runner.get();

		Runnable wrappedCommand = () -> {
			runner.set(new ActionRunner(parentRunner, Thread.currentThread()));
			if (parentRunner != null)
				parentRunner.children.add(runner.get());

			try {
				command.run();
			} catch (Throwable e) {
				// interrupt the entire hierarchy
				try {
					runner.get().interruptHierarchy();
				} catch (InterruptedException ex) {

				}

				var rootCause = Throwables.getRootCause(e);
				if (rootCause instanceof HardwareException) {
					e.printStackTrace(); // cancelling the action should suffice
				} else if (rootCause instanceof InterruptedException) {
					// ignore
				} else
					sneakyThrow(rootCause); // pass it to the orchestrator to deal with
			} finally {
				runner.get().complete();
				runner.remove();
			}
		};

		super.execute(wrappedCommand);
	}
}
