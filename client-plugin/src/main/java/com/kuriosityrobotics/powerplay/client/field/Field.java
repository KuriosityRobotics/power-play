package com.kuriosityrobotics.powerplay.client.field;

import static java.lang.Math.abs;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;
import com.kuriosityrobotics.powerplay.client.Orchestrators;
import com.kuriosityrobotics.powerplay.localisation.messages.LocalisationDatum;
import com.kuriosityrobotics.powerplay.math.Pose;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.Topic;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import java.awt.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.*;

public class Field extends JBPanel<Field> {
	// TODO:  graphical configuration of robot size
	private static final double FIELD_WIDTH = 144, FIELD_HEIGHT = 144;
	private static final double ROBOT_WIDTH = 12, ROBOT_HEIGHT = 12;
	public static final Color[] colours = {
		JBColor.RED, JBColor.BLUE, JBColor.GREEN, JBColor.YELLOW, JBColor.CYAN, JBColor.MAGENTA
	};

	private final Topic<? extends LocalisationDatum> localisationData;

	public Field(Orchestrator orchestrator) {
		this.localisationData = orchestrator.getOrAddTopic("localisation", LocalisationDatum.class);
		orchestrator.subscribe(
				"localisation",
				LocalisationDatum.class,
				data -> {
					if (!repaintLock.isLocked()) {
						repaintLock.lock();
						repaint();
						repaintLock.unlock();
					}
				});

		//		addKeyListener(new ArrowKeyController(orchestrator));
		//		setFocusable(true);
		setEnabled(true);
	}

	private final ReentrantLock repaintLock = new ReentrantLock();

	/**
	 * Draws a 12x12 field and robot, where (0, 0) is at the bottom left. +x is right, +y is up.
	 *
	 * @param graphics
	 */
	private void drawGrid(Graphics2D graphics) {
		// Draw a rectangle for the field.
		graphics.setColor(JBColor.WHITE);
		graphics.drawRect(0, 0, getWidth(), getHeight());

		localisationData
				.lastValues()
				.forEach((details, location) -> drawRobot(graphics, details, location.pose()));

		// Draw a grid.
		graphics.setColor(JBColor.GRAY);
		for (int i = 0; i <= FIELD_WIDTH; i += 12)
			graphics.drawLine(
					(int) (getWidth() * i / FIELD_WIDTH),
					0,
					(int) (getWidth() * i / FIELD_WIDTH),
					getHeight());
		for (int i = 0; i <= FIELD_HEIGHT; i += 12)
			graphics.drawLine(
					0,
					(int) (getHeight() * i / FIELD_HEIGHT),
					getWidth(),
					(int) (getHeight() * i / FIELD_HEIGHT));

		// Draw grid marks and labels.
		graphics.setColor(JBColor.BLACK);
		for (int i = 0; i <= FIELD_WIDTH; i += 12) {
			graphics.drawLine(
					(int) (getWidth() * i / FIELD_WIDTH),
					0,
					(int) (getWidth() * i / FIELD_WIDTH),
					getHeight());
			graphics.drawString(
					String.format("%d", i), (int) (getWidth() * i / FIELD_WIDTH), getHeight() / 2);
		}
		for (int i = 0; i <= FIELD_HEIGHT; i += 12) {
			graphics.drawLine(
					0,
					(int) (getHeight() * i / FIELD_HEIGHT),
					getWidth(),
					(int) (getHeight() * i / FIELD_HEIGHT));
			graphics.drawString(
					String.format("%d", i), getWidth() / 2, (int) (getHeight() * i / FIELD_HEIGHT));
		}
	}

	private void drawRobot(Graphics2D graphics, RobotDetails robotDetails, Pose robotPosition) {
		if (!Orchestrators.CONNECTED.contains(robotDetails)) return;

		// Draw a rotated rectangle for the robot.
		var robotLocationPixels = getLocationPixels(robotPosition);
		var robotWidthPixels = getWidth() * ROBOT_WIDTH / FIELD_WIDTH;
		var robotHeightPixels = getHeight() * ROBOT_HEIGHT / FIELD_HEIGHT;
		graphics.translate(robotLocationPixels.getX(), robotLocationPixels.getY());

		graphics.setColor(JBColor.BLACK);
		graphics.drawString(
				robotDetails.toString(),
				(int) (-robotWidthPixels / 2),
				(int) (-robotHeightPixels / 2f) - 10);

		graphics.setColor(colours[abs(robotDetails.nameBasedHashCode()) % colours.length]);
		graphics.rotate(-robotPosition.orientation());
		graphics.fillRect(
				-(int) (robotWidthPixels / 2),
				-(int) (robotHeightPixels / 2),
				(int) robotWidthPixels,
				(int) robotHeightPixels);
		graphics.setColor(JBColor.BLACK);
		// Line from the middle of the robot to the front of the robot.
		var oldStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(5));
		graphics.drawLine(0, 0, 0, -(int) (robotHeightPixels / 2));
		graphics.rotate(robotPosition.orientation());
		graphics.translate(-robotLocationPixels.getX(), -robotLocationPixels.getY());
		graphics.setStroke(oldStroke);
	}

	/**
	 * Uses the component width to find the pixel position of a robot given its physical position
	 * (0, 0) is bottom left, +x is right, +y is up
	 *
	 * @param robotPosition
	 */
	private Point getLocationPixels(Pose robotPosition) {
		var x = robotPosition.x();
		var y = robotPosition.y();
		var xPixels = (int) (-y * getWidth() / FIELD_WIDTH);
		var yPixels = (int) ((FIELD_HEIGHT - x) * getHeight() / FIELD_HEIGHT);
		return new Point(xPixels, yPixels);
	}

	@Override
	public void paint(Graphics g) {
		setSize(Math.min(getWidth(), getHeight()), Math.min(getWidth(), getHeight()));

		super.paint(g);

		var graphics = (Graphics2D) g;
		drawGrid(graphics);
	}
}
