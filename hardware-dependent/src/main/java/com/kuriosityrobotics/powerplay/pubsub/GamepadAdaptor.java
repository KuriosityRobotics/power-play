package com.kuriosityrobotics.powerplay.pubsub;

import com.kuriosityrobotics.powerplay.pubsub.Node;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Publisher;
import com.kuriosityrobotics.powerplay.pubsub.annotation.Hidden;
import com.kuriosityrobotics.powerplay.pubsub.annotation.RunPeriodically;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Func;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This node translates an FTC SDK gamepad to messages on topics <code>[parentTopic]/a</code>,
 * <code>[parentTopic]/leftStickX</code> etc
 */
@Hidden
public class GamepadAdaptor {
	private final Orchestrator orchestrator;

	private final Set<Field> continuousControls;
	private final Set<Field> buttons;

	/**
	 * This node translates an FTC SDK gamepad to messages on topics
	 *
	 * @param orchestrator the orchestrator to use
	 */
	private GamepadAdaptor(Orchestrator orchestrator) {
		this.orchestrator = orchestrator;

		this.continuousControls = new HashSet<>();
		for (var field : Gamepad.class.getFields()) {
			if (field.getType() == float.class) {
				continuousControls.add(field);
			}
		}

		this.buttons = new HashSet<>();
		for (var f : Gamepad.class.getFields()) {
			if (f.getType() == boolean.class) {
				buttons.add(f);
			}
		}
	}

	private final Gamepad lastGamepad = new Gamepad();

	private void dispatch(Gamepad gamepad, String parentTopic) {
		try {
			// make a copy of the gamepad state to avoid race conditions on the rising/falling edges
			for (var field : buttons) {
				var lastValue = field.getBoolean(lastGamepad);
				var newValue = field.getBoolean(gamepad);

				if (!lastValue && newValue) { // rising
					orchestrator.dispatch(parentTopic + "/" + field.getName() + "/rising", "");
				} else if (lastValue && !newValue) {
					orchestrator.dispatch(parentTopic + "/" + field.getName() + "/falling", "");
				}

				orchestrator.dispatch(parentTopic + "/" + field.getName(), newValue);
			}

			for (var field : continuousControls) {
				orchestrator.dispatch(parentTopic + "/" + field.getName(), field.getDouble(gamepad));
			}

			lastGamepad.copy(gamepad);
		} catch (IllegalAccessException e) {
			orchestrator.err(e);
		}
	}

	public static Gamepad hookableGamepad(Orchestrator orchestrator, String parentTopic) {
		var adaptor = new GamepadAdaptor(orchestrator);
		return new Gamepad() {
			@Override
			public void copy(Gamepad gamepad) {
				super.copy(gamepad);
				adaptor.dispatch(gamepad, parentTopic);
			}
		};
	}

	/**
	 * Starts a node that regularly transmits data from this gamepad
	 * @param opmodeGamepad the source gamepad
	 * @param parentTopic the topic under which the individual gamepad buttons will be transmitted
	 */
	public static void hookGamepad(Orchestrator orchestrator, Gamepad opmodeGamepad, String parentTopic) {
		var adaptor = new GamepadAdaptor(orchestrator);
		var node = new gamepadNode(orchestrator, adaptor,opmodeGamepad, parentTopic);

		orchestrator.startNode(parentTopic, node);
	}

	@Hidden
	static class gamepadNode extends Node {
		private final GamepadAdaptor adaptor;
		private final Gamepad opmodeGamepad;
		private final String parentTopic;

		public gamepadNode(Orchestrator orchestrator, GamepadAdaptor adaptor, Gamepad opmodeGamepad, String parentTopic) {
			super(orchestrator);
			this.adaptor = adaptor;
			this.opmodeGamepad = opmodeGamepad;
			this.parentTopic = parentTopic;
		}

		@RunPeriodically(maxFrequency = 10)
		void update() {
			adaptor.dispatch(opmodeGamepad, parentTopic);
		}
	}
}
