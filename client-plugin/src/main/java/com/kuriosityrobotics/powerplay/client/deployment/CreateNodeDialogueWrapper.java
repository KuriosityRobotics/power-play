package com.kuriosityrobotics.powerplay.client.deployment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.kuriosityrobotics.powerplay.pubsub.Orchestrator;
import com.kuriosityrobotics.powerplay.pubsub.bridge.message.NodeInfo;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;
import org.jetbrains.annotations.Nullable;

public class CreateNodeDialogueWrapper extends DialogWrapper {
	private final Orchestrator orchestrator;
	private final CreateNodeDialogueCentrePanel northPanel;

	protected CreateNodeDialogueWrapper(Project project, Orchestrator orchestrator) {
		super(project);
		this.orchestrator = orchestrator;
		this.northPanel = new CreateNodeDialogueCentrePanel();
		try {
			var method = northPanel.getClass().getDeclaredMethod("$$$setupUI$$$");
			method.setAccessible(true);
			method.invoke(northPanel);
		} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		setOKActionEnabled(true);
		setModal(true);
		setTitle("Start Node");
		setSize(300, 150);
		init();
	}

	@Override
	protected @Nullable JComponent createCenterPanel() {
		return northPanel.panel1;
	}

	private NodeInfo createdNode;

	@Override
	protected void doOKAction() {
		var nodeType = northPanel.nodeType();
		var nodeName = northPanel.nodeName();

		createdNode = new NodeInfo(nodeType, nodeName);
		orchestrator.fireMessageAt(createdNode, "node/requestStart", northPanel.robotDeployment());
		super.doOKAction();
	}

	public NodeInfo getCreatedNode() {
		return createdNode;
	}
}
