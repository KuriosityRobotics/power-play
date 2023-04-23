package com.kuriosityrobotics.powerplay.client.telemetry;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import com.kuriosityrobotics.powerplay.client.Orchestrators;
import org.jetbrains.annotations.NotNull;

public class TelemetryToolWindowFactory implements ToolWindowFactory {
	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		toolWindow.setTitle("Telemetry");

		var toolWindowContent = new TelemetryGui(Orchestrators.CLIENT);
		var content =
				ContentFactory.SERVICE
						.getInstance()
						.createContent(toolWindowContent, "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
