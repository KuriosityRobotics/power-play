package com.kuriosityrobotics.powerplay.pubsub;

import static com.kuriosityrobotics.powerplay.pubsub.OrchestratorImpl.sneakyThrow;

import com.kuriosityrobotics.powerplay.hardware.MotorFaults;
import com.kuriosityrobotics.powerplay.hardware.RobotConstants;
import com.qualcomm.hardware.lynx.LynxAnalogInputController;
import com.qualcomm.hardware.lynx.LynxController;
import com.qualcomm.hardware.lynx.LynxDcMotorController;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxNackException;
import com.qualcomm.hardware.lynx.LynxServoController;
import com.qualcomm.hardware.lynx.commands.core.LynxSetMotorChannelModeCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxSetServoEnableCommand;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.AnalogInputController;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorControllerEx;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorImplEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.ServoControllerEx;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType;
import com.qualcomm.robotcore.util.LastKnown;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HardwareProviderImpl implements HardwareProvider {
	private final Orchestrator orchestrator;
	private final HardwareMap hardwareMap;

	public HardwareProviderImpl(Orchestrator orchestrator, HardwareMap hardwareMap) {
		this.orchestrator = orchestrator;
		this.hardwareMap = hardwareMap;
	}


	private final Map<RobotConstants.LynxHub, LynxModule> modules = new ConcurrentHashMap<>();
	private final Map<LynxModule, LynxDcMotorController> motorControllers = new ConcurrentHashMap<>();
	private final Map<LynxModule, AnalogInputController> analogInputControllers = new ConcurrentHashMap<>();
	private final Map<LynxModule, ServoControllerEx> servoControllers = new ConcurrentHashMap<>();

	@Override
	public LynxModule moduleFor(RobotConstants.LynxHub hub) {
		if (!modules.containsKey(hub)) {
			var lynxModule = hardwareMap.get(LynxModule.class, hub.hardwareName());
			modules.put(hub, lynxModule);
		}

		return modules.get(hub);
	}

	@Override
	public DcMotorControllerEx motorControllerFor(RobotConstants.LynxHub hub) {
		var module = moduleFor(hub);
		return motorControllers.computeIfAbsent(
			module,
			m -> {
				try {
					var result = new LynxDcMotorController(hardwareMap.appContext, m) {
						@Override
						public void disengage() {
							RobotLog.vv(getTag(), "disengage mod#=%d", getModule().getModuleAddress());
							isEngaged = false;
							adjustHookingToMatchEngagement();
						}

						@Override
						public void engage() {
							RobotLog.vv(getTag(), "engaging mod#=%d", getModule().getModuleAddress());
							isEngaged = true;
							adjustHookingToMatchEngagement();
						}

						@Override
						public boolean isMotorOverCurrent(int motor) {
							var lv = orchestrator.lastValue("motorFaults", MotorFaults.class).getValue();
							return lv != null && lv.forHub(hub)[motor];
						}
					};

					for (int i = 0; i < 4; i++) {
						result.setMotorCurrentAlert(i, 10, CurrentUnit.AMPS); // a bit obscene, but it will avoid false positives
					}

					return result;
				} catch (RobotCoreException | InterruptedException e) {
					e.printStackTrace();
					return null;
				}
			}
		);
	}

	@Override
	public AnalogInputController analogInputControllerFor(RobotConstants.LynxHub hub) {
		var module = moduleFor(hub);
		return analogInputControllers.computeIfAbsent(
			module,
			m -> {
				try {
					return new LynxAnalogInputController(hardwareMap.appContext, m);
				} catch (RobotCoreException | InterruptedException e) {
					e.printStackTrace();
					return null;
				}
			}
		);
	}

	@Override
	public ServoControllerEx servoControllerFor(RobotConstants.LynxHub hub) {
		var module = moduleFor(hub);
		return servoControllers.computeIfAbsent(
			module,
			m -> {
				try {
					return new LynxServoController(hardwareMap.appContext, m);
				} catch (RobotCoreException | InterruptedException e) {
					e.printStackTrace();
					return null;
				}
			}
		);
	}

	@Override
	public DcMotorEx motor(RobotConstants.LynxHub hub, int portNumber, DcMotorSimple.Direction direction) {
		return new DcMotorImplEx(motorControllerFor(hub), portNumber, direction) {
			@Override
			public void setPower(double power) {
				if (controller instanceof LynxDcMotorController && !((LynxDcMotorController) controller).isEngaged())
					return;

				super.setPower(power);
			}
		};
	}

	@Override
	public ServoImplEx servo(RobotConstants.LynxHub hub, int portNumber) {
		return new ServoImplEx(servoControllerFor(hub), portNumber, ServoConfigurationType.getStandardServoType());
	}

	@Override
	public AnalogInput analogInput(RobotConstants.LynxHub hub, int channel) {
		return new com.qualcomm.robotcore.hardware.AnalogInput(analogInputControllerFor(hub), channel);
	}

	@Override
	public <T extends HardwareDevice> T byName(Class<? extends T> classOrInterface, String deviceName) {
		return hardwareMap.get(classOrInterface, deviceName);
	}

	@Override
	public <T extends HardwareDevice> T byName(String deviceName) {
		return (T) hardwareMap.get(deviceName);
	}

	@Override
	public void arm() {
		for (var controller : motorControllers.values()) {
			controller.engage();
			controller.forgetLastKnown();
		}

		for (var controller : servoControllers.values()) {
			controller.pwmEnable();
			((LynxServoController)controller).engage();
		}
	}

	@Override
	public void disarm() {
		try {
			for (var entry : motorControllers.entrySet())
				disarmMotors(entry.getKey(), entry.getValue());

			for (var entry : servoControllers.entrySet())
				disarmServos(entry.getKey(), (LynxServoController) entry.getValue());
		} catch (NoSuchFieldException | IllegalAccessException | RobotCoreException | InterruptedException | LynxNackException e) {
			sneakyThrow(e);
		}
	}

	private void disarmMotors(LynxModule module, LynxDcMotorController controller) throws NoSuchFieldException, IllegalAccessException, RobotCoreException, InterruptedException, LynxNackException {
		{
			// hack to disengage the controllers w/o acquiring the monitor
			var isEngaged = LynxController.class.getDeclaredField("isEngaged");
			var isHooked = LynxController.class.getDeclaredField("isHooked");

			isEngaged.setAccessible(true);
			isHooked.setAccessible(true);

			isEngaged.setBoolean(controller, false);
			isHooked.setBoolean(controller, false);
			controller.forgetLastKnown();
		}

		{
			var motorsField = LynxDcMotorController.class.getDeclaredField("motors");
			var lastKnownMode = LynxDcMotorController.class.getDeclaredClasses()[0].getDeclaredField("lastKnownMode");

			motorsField.setAccessible(true);
			lastKnownMode.setAccessible(true);

			for (int i = LynxDcMotorController.apiMotorFirst; i <= LynxDcMotorController.apiMotorLast; i++) {
				var lmk = (LastKnown<DcMotor.RunMode>) lastKnownMode.get(Array.get(motorsField.get(controller), i));
				var runmode = lmk.getValue();
				if (runmode == null) runmode = DcMotor.RunMode.RUN_WITHOUT_ENCODER; // default mode?

				var command = new LynxSetMotorChannelModeCommand(module, i, runmode, DcMotor.ZeroPowerBehavior.FLOAT);
				command.send();
			}
		}
	}

	private void disarmServos(LynxModule module, LynxServoController controller) throws IllegalAccessException, NoSuchFieldException, RobotCoreException, InterruptedException, LynxNackException {
		// hack to disengage the controllers w/o acquiring the monitor
		var isEngaged = LynxController.class.getDeclaredField("isEngaged");
		var isHooked = LynxController.class.getDeclaredField("isHooked");

		isEngaged.setAccessible(true);
		isHooked.setAccessible(true);

		// 1.  Disengage the controller object s.t. nodes won't affect the physical system
		isEngaged.setBoolean(controller, false);
		isHooked.setBoolean(controller, false);
		controller.forgetLastKnown();

		// 2.  Construct a temporary controller to disable the motors
		for (int i = LynxServoController.apiServoFirst; i <= LynxServoController.apiServoLast; i++)
			new LynxSetServoEnableCommand(module, i, false).send();
	}
}