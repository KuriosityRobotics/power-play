package com.kuriosityrobotics.powerplay.client.deployment;

import com.intellij.openapi.project.Project;
import com.intellij.ui.*;
import com.intellij.ui.components.panels.HorizontalBox;
import com.kuriosityrobotics.powerplay.client.Orchestrators;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.NodeInfo;
import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.*;
import org.jetbrains.annotations.Nullable;

public class NodeDeploymentEditPanel extends AddDeleteListPanel<NodeInfo> {
	private final Project project;
	private final Orchestrator orchestrator;

	public NodeDeploymentEditPanel(Project project, Orchestrator orchestrator) {
		super("", new ArrayList<>());
		this.project = project;
		this.orchestrator = orchestrator;

		try {
			var field = myListModel.getClass().getDeclaredField("delegate");
			field.setAccessible(true);
			Vector<NodeInfo> newVec = new VecExt();
			field.set(myListModel, newVec);
			myList.setModel(myListModel);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		Orchestrators.CONNECTED.forEach(
				n -> {
					myListModel.addAll(n.runningNodes());
				});

		Orchestrators.addRobotAddedListener(
				deets -> {
					myListModel.addAll(deets.runningNodes());
				});
		Orchestrators.addRobotRemovedListener(
				deets -> {
					deets.runningNodes().forEach(myListModel::removeElement);
				});

		Orchestrators.CLIENT.subscribe(
				"node/started",
				NodeInfo.class,
				(nodeInfo, _topic, deets) -> {
					myListModel.addElement(nodeInfo);
				});

		myList.installCellRenderer(
				node -> {
					var typeLabel = new JLabel(getSimpleName(node.nodeType()), JLabel.CENTER);
					typeLabel.setForeground(JBColor.lightGray);

					var nameLabel = new JLabel(node.toString(), JLabel.CENTER);
					nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

					var box = new HorizontalBox();
					box.add(nameLabel);
					box.add(Box.createHorizontalStrut(5));
					box.add(typeLabel);

					return box;
				});
	}

	static String getSimpleName(String fullyQualifiedName) {
		var split = fullyQualifiedName.split("\\.");
		return split[split.length - 1];
	}

	@Override
	protected @Nullable NodeInfo findItemToAdd() {
		var wrapper = new CreateNodeDialogueWrapper(project, orchestrator);
		wrapper.showAndGet();
		return null;
	}

	@Override
	protected void customizeDecorator(ToolbarDecorator decorator) {
		super.customizeDecorator(decorator);
	}

	public class VecExt extends Vector<NodeInfo> {

		public VecExt() {}

		@Override
		public synchronized NodeInfo remove(int index) {
			var node = super.remove(index);
			orchestrator.stopNode(node.nodeName());
			return node;
		}

		@Override
		public synchronized void removeElementAt(int index) {
			var node = get(index);
			orchestrator.stopNode(node.nodeName());

			super.removeElementAt(index);
		}
	}
}
