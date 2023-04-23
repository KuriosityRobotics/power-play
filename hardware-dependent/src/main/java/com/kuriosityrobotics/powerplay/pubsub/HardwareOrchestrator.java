package com.kuriosityrobotics.powerplay.pubsub;

import android.util.Log;

import com.kuriosityrobotics.powerplay.bulkdata.BulkDataFetcher;
import com.kuriosityrobotics.powerplay.drive.DrivetrainController;
import com.kuriosityrobotics.powerplay.io.IntakeOuttakeNode;
import com.kuriosityrobotics.powerplay.localisation.IMUNode;
import com.kuriosityrobotics.powerplay.localisation.LiftingOdoNode;
import com.kuriosityrobotics.powerplay.localisation.odometry.Odometry;
import com.kuriosityrobotics.powerplay.localisation.odometry.OdometryIntegrator;
import com.kuriosityrobotics.powerplay.mpc.MPCNode;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.io.PrintStream;
import java.util.function.Consumer;

public class HardwareOrchestrator extends OrchestratorImpl {
	private static final Class[] DEFAULT_NODES = {
		BulkDataFetcher.class,
		LiftingOdoNode.class,
		Odometry.class,
		OdometryIntegrator.class,
		IMUNode.class,
		DrivetrainController.class,
		IntakeOuttakeNode.class,
	};

	final HardwareProviderImpl hardwareProvider;
	private final HardwareAnnotationBinder hardwareAnnotationBinder;

	final Object exitLock;

	public HardwareOrchestrator(RobotDetails robotDetails, boolean debugMode, boolean startNetwork, HardwareMap hardwareMap) {
		super(robotDetails, debugMode, startNetwork, hardwareMap);
		info("HardwareOrchestrator constructed");
		this.hardwareProvider = new HardwareProviderImpl(this, hardwareMap);
		this.hardwareAnnotationBinder = new HardwareAnnotationBinder(this, hardwareProvider);
		this.exitLock = new Object();
	}

	private Throwable pendingException = null;

	@Override
	protected void onUncaughtException(Thread source, Throwable t) {
		try {
			pendingException = t;
			super.onUncaughtException(source, t);
		} finally {
			close();
		}
	}

	public void waitForOpmodeEnd() {
		try {
			synchronized (exitLock) {
				exitLock.wait();
			}
		} catch (InterruptedException ignored) {
		} finally {
			if (pendingException != null)
				sneakyThrow(pendingException);
		}
	}

	public HardwareProviderImpl getHardwareProvider() {
		return hardwareProvider;
	}

	@Override
	public void markNodeBeingConstructed(Node node) {
		try {
			if (hardwareAnnotationBinder != null)
				hardwareAnnotationBinder.bind(node);
		} catch (Throwable e) {
			sneakyThrow(e);
		}
		super.markNodeBeingConstructed(node);
	}

	public void startDefaultNodes() {
		info("Starting default nodes");
		for (Class defaultNode : DEFAULT_NODES) {
			try {
				var constructor = defaultNode.getConstructor(Orchestrator.class);
				startNode(defaultNode.getSimpleName(), (Node) constructor.newInstance(this));
			} catch (Throwable e) {
				throw new RuntimeException("Error constructing " + defaultNode.getSimpleName() + ": " + (e.getCause() == null ? e : e.getCause()));
			}
		}
	}

	 static HardwareOrchestrator create(HardwareMap hardwareMap, boolean debug) {
		return new HardwareOrchestrator(
			new RobotDetails("Physical Robot", random.nextLong()),
			debug, debug,
			hardwareMap
		);

	}

	 static HardwareOrchestrator create(HardwareMap hardwareMap) {
		return create(hardwareMap, false);
	}

	 public static HardwareOrchestrator create(OpMode opMode) {
		return create(opMode.hardwareMap, false);
	}

	@Override
	protected Consumer<String> getErrorStream() {
		return s -> Log.e("Orchestrator", s);
	}

	@Override
	protected Consumer<String> getErrorStream(Node originatingNode) {
		return s -> Log.e(getNodeName(originatingNode), s);
	}

	@Override
	protected Consumer<String> getWarnStream() {
		return s -> Log.w("Orchestrator", s);
	}

	@Override
	protected Consumer<String> getWarnStream(Node originatingNode) {
		return s -> Log.w(getNodeName(originatingNode), s);
	}

	@Override
	protected Consumer<String> getInfoStream() {
		return s -> Log.i("Orchestrator", s);
	}

	@Override
	protected Consumer<String> getInfoStream(Node originatingNode) {
		return s -> Log.i(getNodeName(originatingNode), s);
	}

	@Override
	protected Consumer<String> getDebugStream() {
		return s -> Log.d("Orchestrator", s);
	}

	@Override
	protected Consumer<String> getDebugStream(Node originatingNode) {
		return s -> Log.d(getNodeName(originatingNode), s);
	}

	@Override
	public void close() {
		hardwareProvider.disarm();
		super.close();

		synchronized (exitLock) {
			exitLock.notifyAll();
		}
	}

	@Override
	public void info(String msg, Node originatingNode) {
		Log.i(originatingNode.getClass().getSimpleName(), msg);
	}

	@Override
	public void err(String msg, Node originatingNode) {
		Log.e(originatingNode.getClass().getSimpleName(), msg);
	}

	@Override
	public void err(Throwable e, Node originatingNode) {
		Log.e(originatingNode.getClass().getSimpleName(), "uncaught exception", e);
	}

	@Override
	public void warn(String msg, Node originatingNode) {
		Log.w(originatingNode.getClass().getSimpleName(), msg);
	}

	@Override
	public void warn(Throwable e, Node originatingNode) {
		Log.w(originatingNode.getClass().getSimpleName(), "uncaught exception", e);
	}

	@Override
	public void debug(String msg, Node originatingNode) {
		Log.d(originatingNode.getClass().getSimpleName(), msg);
	}
}
