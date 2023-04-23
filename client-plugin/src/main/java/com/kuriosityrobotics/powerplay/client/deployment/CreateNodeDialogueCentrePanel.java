package com.kuriosityrobotics.powerplay.client.deployment;

import com.intellij.openapi.ui.ComboBox;
import com.kuriosityrobotics.powerplay.client.Orchestrators;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.RobotDetails;
import java.awt.*;
import java.awt.event.ItemEvent;
import javax.swing.*;

public class CreateNodeDialogueCentrePanel {
	private JTextField nodeNameTextField;
	public JPanel panel1;
	private ComboBox<String> nodeTypeBox;
	private ComboBox<RobotDetails> robotDeploymentBox;

	private void createUIComponents() {
		nodeTypeBox = new ComboBox<>();
		robotDeploymentBox = new ComboBox<>();

		Orchestrators.CONNECTED.forEach(robotDeploymentBox::addItem);
		Orchestrators.addRobotAddedListener(robotDeploymentBox::addItem);
		Orchestrators.addRobotRemovedListener(robotDeploymentBox::removeItem);

		nodeTypeBox.setRenderer(
				new DefaultListCellRenderer() {
					@Override
					public Component getListCellRendererComponent(
							JList<?> list,
							Object value,
							int index,
							boolean isSelected,
							boolean cellHasFocus) {
						super.getListCellRendererComponent(
								list, value, index, isSelected, cellHasFocus);
						if (value instanceof String) {
							setText(NodeDeploymentEditPanel.getSimpleName((String) value));
						}
						return this;
					}
				});
		robotDeploymentBox.addItemListener(
				e -> {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						nodeTypeBox.removeAllItems();
						robotDeploymentBox
								.getItem()
								.getInstalledNodes()
								.forEach(nodeTypeBox::addItem);
					}
				});
		robotDeploymentBox.getItem().getInstalledNodes().forEach(nodeTypeBox::addItem);
	}

	public String nodeType() {
		return nodeTypeBox.getItem();
	}

	public RobotDetails robotDeployment() {
		return robotDeploymentBox.getItem();
	}

	public String nodeName() {
		return nodeNameTextField.getText();
	}
}
