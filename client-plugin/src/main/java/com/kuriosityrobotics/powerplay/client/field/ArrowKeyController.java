package com.kuriosityrobotics.powerplay.client.field;

import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArrowKeyController implements KeyListener {
	private final Orchestrator orchestrator;
	private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();

	@Override
	public void keyTyped(KeyEvent keyEvent) {}

	@Override
	public void keyPressed(KeyEvent e) {
		pressedKeys.add(e.getKeyCode());
		dispatchAccordingToKeyCodes();
	}

	private void dispatchAccordingToKeyCodes() {
		if (isKeyHeld(KeyEvent.VK_UP)) {orchestrator.dispatch("gamepad1/leftStickY", 1.f); System.out.println("copioum");}
		else if (isKeyHeld(KeyEvent.VK_DOWN)) orchestrator.dispatch("gamepad1/leftStickY", -1.f);
		else orchestrator.dispatch("gamepad1/leftStickY", 0.f);

		if (isKeyHeld(KeyEvent.VK_RIGHT)) orchestrator.dispatch("gamepad1/leftStickX", 1.f);
		else if (isKeyHeld(KeyEvent.VK_LEFT)) orchestrator.dispatch("gamepad1/leftStickX", -1.f);
		else orchestrator.dispatch("gamepad1/leftStickX", 0.f);

		if (isKeyHeld(KeyEvent.VK_D)) orchestrator.dispatch("gamepad1/rightStickX", 1.f);
		else if (isKeyHeld(KeyEvent.VK_A)) orchestrator.dispatch("gamepad1/rightStickX", -1.f);
		else orchestrator.dispatch("gamepad1/rightStickX", 0.f);
	}

	public ArrowKeyController(Orchestrator orchestrator) {
		this.orchestrator = orchestrator;
		//		IdeEventQueue.getInstance().addDispatcher(event -> {
		//			if (event.getID() == KeyEvent.KEY_PRESSED) {
		//				keyPressed(((KeyEvent) event));
		//			}
		//			else if (event.getID() == KeyEvent.KEY_RELEASED) {
		//				keyReleased(((KeyEvent) event));
		//			}
		//			else if (event.getID() == KeyEvent.KEY_TYPED) {
		//				keyTyped(((KeyEvent) event));
		//			}
		//			var code = ((KeyEvent)event).getKeyCode();
		//			return code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_LEFT
		// ||
		// code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_A || code == KeyEvent.VK_D;
		//		}, null);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		pressedKeys.remove(e.getKeyCode());
		dispatchAccordingToKeyCodes();
	}

	public boolean isKeyHeld(int keyCode) {
		return pressedKeys.contains(keyCode);
	}
}
