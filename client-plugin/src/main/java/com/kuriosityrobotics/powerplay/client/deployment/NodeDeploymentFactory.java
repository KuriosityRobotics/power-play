package com.kuriosityrobotics.powerplay.client.deployment;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.kuriosityrobotics.powerplay.client.Orchestrators;
import org.jetbrains.annotations.NotNull;

public class NodeDeploymentFactory implements ToolWindowFactory {
	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		toolWindow.setTitle("Node Deployment");

		var toolWindowContent = new NodeDeploymentEditPanel(project, Orchestrators.CLIENT);
		var content =
				ContentFactory.SERVICE.getInstance().createContent(toolWindowContent, "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
